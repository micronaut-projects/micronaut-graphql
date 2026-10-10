/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.graphql;

import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.micronaut.configuration.graphql.ws.GraphQLWsConfiguration;
import io.micronaut.context.BeanContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.context.WatchableBeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.env.DevelopmentActive;
import io.micronaut.context.reload.ClassChangeEvent;
import io.micronaut.context.reload.ReloadStrategy;
import io.micronaut.context.reload.ResourceKind;
import io.micronaut.context.watch.BeanDefinitionChange;
import io.micronaut.context.watch.ConfigurationChange;
import io.micronaut.context.watch.ReloadingConfigurationWatcher;
import io.micronaut.context.watch.ResourceChange;
import io.micronaut.core.annotation.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Recreates the {@link GraphQL} bean, and what it is built from, in development mode when that changes. It exists only
 * in development mode, so nothing of it is on the path of a request.
 *
 * <ul>
 *     <li>A change of a schema file ({@code .graphqls}, {@code .graphql} or {@code .gqls}) among the resources the
 *     development runtime watches recreates the schema beans and the {@link GraphQL} bean, which read the files when
 *     they are created.</li>
 *     <li>A {@link DataFetcher}, {@link RuntimeWiring}, {@link TypeDefinitionRegistry}, {@link GraphQLSchema} or
 *     {@link GraphQLCodeRegistry} definition registered or removed recreates the schema beans and the {@link GraphQL}
 *     bean, so that the next one is built from the current definitions.</li>
 *     <li>A class change applied in place that retires a classloader recreates the data fetchers too: they would hold,
 *     and run, classes of the retired generation.</li>
 *     <li>A change of the configuration under {@code graphql} recreates the {@link GraphQL} bean, which an application
 *     may build from it. A change of a path or of a switch that adds or removes an endpoint asks for a restart instead:
 *     it adds, moves or removes a route. So does a change under {@code graphql.graphiql} once the GraphiQL controller
 *     was created: it renders its page once, and its route keeps the instance it resolved first.</li>
 * </ul>
 *
 * <p>The schema beans are the {@link TypeDefinitionRegistry}, {@link RuntimeWiring}, {@link GraphQLSchema} and
 * {@link GraphQLCodeRegistry} singletons the context holds. Each bean is recreated through
 * {@link WatchableBeanContext#recreate(Object)}, which destroys the beans that received it. Nothing is created that was
 * not created already: a bean nobody asked for yet is built from the current state when it is first asked for. The
 * {@link DefaultGraphQLInvocation} looks the {@link GraphQL} bean up for each operation, so the next request, and the
 * next operation of an open GraphQL over WebSocket connection, is executed by the new one; a subscription already
 * running keeps the publisher it was given.</p>
 *
 * <p>It holds the context only, never a GraphQL bean: a bean that received one is a dependent of it, which recreating
 * it would destroy along with its watches.</p>
 *
 * @author graemerocher
 * @since 5.2.0
 */
@Internal
@Context
@DevelopmentActive
final class DevelopmentGraphQLReloader {

    /**
     * The extensions of GraphQL schema files.
     */
    private static final String[] SCHEMA_FILES = {"**/*.graphqls", "**/*.graphql", "**/*.gqls"};

    /**
     * The resource kinds a schema file may be found under.
     */
    private static final List<ResourceKind> SCHEMA_KINDS = List.of(ResourceKind.CONFIG, ResourceKind.OTHER);

    /**
     * The types the {@link GraphQL} bean is built from, besides the data fetchers.
     */
    private static final List<Class<?>> SCHEMA_TYPES = List.of(
        TypeDefinitionRegistry.class,
        RuntimeWiring.class,
        GraphQLSchema.class,
        GraphQLCodeRegistry.class
    );

    /**
     * The properties that decide whether an endpoint exists, and where: a change adds, moves or removes a route.
     */
    private static final String[] ROUTES = {
        GraphQLConfiguration.ENABLED_CONFIG,
        GraphQLConfiguration.PATH_CONFIG,
        GraphQLConfiguration.GraphiQLConfiguration.ENABLED_CONFIG,
        GraphQLConfiguration.PREFIX + "." + GraphQLConfiguration.GraphiQLConfiguration.PATH_CONFIG,
        GraphQLWsConfiguration.ENABLED_CONFIG,
        GraphQLConfiguration.PREFIX + "." + GraphQLWsConfiguration.PATH_CONFIG
    };

    /**
     * The configuration the GraphiQL page is rendered from.
     */
    private static final String GRAPHIQL = GraphQLConfiguration.PREFIX + "." + GraphQLConfiguration.GraphiQLConfiguration.PREFIX;

    private static final Logger LOG = LoggerFactory.getLogger(DevelopmentGraphQLReloader.class);

    private final BeanContext beanContext;

    /**
     * @param beanContext The context, watched when it can be
     */
    DevelopmentGraphQLReloader(BeanContext beanContext) {
        this.beanContext = beanContext;
        if (beanContext instanceof WatchableBeanContext watchable) {
            // the first batch is what the GraphQL bean was, or will be, built from: only what changes after it matters
            watchable.definitions(DataFetcher.class).watch(change -> {
                if (changed(change)) {
                    recreate(false, "data fetcher definitions changed");
                }
            });
            for (Class<?> type : SCHEMA_TYPES) {
                watchable.definitions(type).watch(change -> {
                    if (changed(change)) {
                        recreate(false, type.getSimpleName() + " definitions changed");
                    }
                });
            }
            for (ResourceKind kind : SCHEMA_KINDS) {
                watchable.resources(kind).include(SCHEMA_FILES).watch(this::onSchemaChange);
            }
            watchable.configuration(GraphQLConfiguration.PREFIX).watchReloading(this::onConfigurationChange);
            watchable.classChanges().watch(this::onClassChange);
        }
    }

    private void onSchemaChange(ResourceChange change) {
        if (!change.initial() && !change.isEmpty()) {
            recreate(false, "the schema files " + change.changed() + change.removed() + " changed");
        }
    }

    private ReloadingConfigurationWatcher.Outcome onConfigurationChange(ConfigurationChange change) {
        if (change.touchesAny(ROUTES)) {
            // a route the router built from the old value: only a new context has the routes the configuration asks for
            LOG.debug("A GraphQL endpoint was enabled, disabled or moved: a restart applies it");
            return ReloadingConfigurationWatcher.Outcome.REQUIRES_RESTART;
        }
        if (change.touches(GRAPHIQL) && !beanContext.getActiveBeanRegistrations(GraphiQLController.class).isEmpty()) {
            // the page is rendered once, and recreating the controller would leave its route on the destroyed instance
            LOG.debug("The GraphiQL configuration changed: a restart renders the page again");
            return ReloadingConfigurationWatcher.Outcome.REQUIRES_RESTART;
        }
        return recreate(false, "the configuration under " + GraphQLConfiguration.PREFIX + " changed");
    }

    private void onClassChange(ClassChangeEvent change) {
        // a restart builds a new context, with a new GraphQL bean
        if (change.strategy() != ReloadStrategy.RESTART && !change.retiredLoaders().isEmpty()) {
            recreate(true, "a reload retired a classloader");
        }
    }

    private static boolean changed(BeanDefinitionChange<?> change) {
        return !change.initial() && (!change.added().isEmpty() || !change.removed().isEmpty());
    }

    /**
     * Recreates the schema beans and the {@link GraphQL} beans the context holds, and when asked the data fetchers.
     *
     * @param dataFetchers Whether to recreate the data fetchers too
     * @param reason Why, for the log
     * @return {@link ReloadingConfigurationWatcher.Outcome#IGNORED} when no such bean was held,
     * {@link ReloadingConfigurationWatcher.Outcome#APPLIED} when they were recreated, and
     * {@link ReloadingConfigurationWatcher.Outcome#REQUIRES_RESTART} when they were held but kept, as by a context
     * that does not track bean dependencies
     */
    private ReloadingConfigurationWatcher.Outcome recreate(boolean dataFetchers, String reason) {
        if (!(beanContext instanceof WatchableBeanContext context)) {
            return ReloadingConfigurationWatcher.Outcome.IGNORED;
        }
        // taken first: recreating one destroys the beans that received it, as the graph records them
        List<Object> beans = new ArrayList<>();
        if (dataFetchers) {
            addActive(beans, DataFetcher.class);
        }
        for (Class<?> type : SCHEMA_TYPES) {
            addActive(beans, type);
        }
        addActive(beans, GraphQL.class);
        if (beans.isEmpty()) {
            return ReloadingConfigurationWatcher.Outcome.IGNORED;
        }
        LOG.debug("Recreating the GraphQL bean: {}", reason);
        boolean recreated = false;
        for (Object bean : beans) {
            // false for a bean destroyed with one recreated before it, and for all of them in a context that does
            // not track bean dependencies: they are kept, and built again after a restart
            recreated |= context.recreate(bean);
        }
        return recreated ? ReloadingConfigurationWatcher.Outcome.APPLIED : ReloadingConfigurationWatcher.Outcome.REQUIRES_RESTART;
    }

    private void addActive(List<Object> beans, Class<?> type) {
        for (BeanRegistration<?> registration : beanContext.getActiveBeanRegistrations(type)) {
            Object bean = registration.bean();
            if (beans.stream().noneMatch(taken -> taken == bean)) {
                beans.add(bean);
            }
        }
    }
}
