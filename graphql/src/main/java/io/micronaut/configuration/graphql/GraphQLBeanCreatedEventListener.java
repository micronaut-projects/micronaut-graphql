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
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLSchema;
import graphql.schema.SingletonPropertyDataFetcher;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;

/**
 * Replaces GraphQL Java's reflective default property fetcher with a Micronaut
 * introspection-aware variant while preserving explicit data fetchers.
 *
 * @since 1.0
 */
@Singleton
final class GraphQLBeanCreatedEventListener implements BeanCreatedEventListener<GraphQL> {

    @Override
    public GraphQL onCreated(BeanCreatedEvent<GraphQL> event) {
        GraphQL graphQL = event.getBean();
        GraphQLSchema graphQLSchema = graphQL.getGraphQLSchema();
        if (!usesSingletonPropertyDataFetcher(graphQLSchema.getCodeRegistry())) {
            return graphQL;
        }
        GraphQLCodeRegistry codeRegistry = graphQLSchema.getCodeRegistry().transform(builder ->
                builder.defaultDataFetcher(MicronautBeanPropertyDataFetcher.factory()));
        GraphQLSchema updatedSchema = graphQLSchema.transform(builder -> builder.codeRegistry(codeRegistry));
        return graphQL.transform(builder -> builder.schema(updatedSchema));
    }

    private boolean usesSingletonPropertyDataFetcher(GraphQLCodeRegistry codeRegistry) {
        return GraphQLCodeRegistry.newCodeRegistry(codeRegistry).getDefaultDataFetcherFactory()
                == SingletonPropertyDataFetcher.singletonFactory();
    }
}
