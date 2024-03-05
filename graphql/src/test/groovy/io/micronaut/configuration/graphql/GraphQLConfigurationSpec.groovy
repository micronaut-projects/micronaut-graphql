/*
 * Copyright 2017-2019 original authors
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

package io.micronaut.configuration.graphql

import graphql.GraphQL
import graphql.schema.GraphQLSchema
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.core.util.StringUtils
import io.micronaut.http.annotation.Controller
import jakarta.inject.Singleton
import spock.lang.Specification

/**
 * @author Marcel Overdijk
 * @author James Kleeh
 * @since 1.0
 */
class GraphQLConfigurationSpec extends Specification {

    void "test no graphql bean provided"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["spec.name": GraphQLConfigurationSpec.simpleName,
                 "graphql.factory": false]
        ))
        context.start()

        expect:
        !context.containsBean(GraphQLExecutionResultHandler)
        !context.containsBean(GraphQLInvocation)
        !context.containsBean(GraphQLController)

        cleanup:
        context.close()
    }

    void "test graphql bean provided"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["spec.name": GraphQLConfigurationSpec.simpleName]
        ))
        context.start()

        expect:
        context.containsBean(GraphQLExecutionResultHandler)
        context.containsBean(GraphQLInvocation)
        context.containsBean(GraphQLController)
        context.getBeanDefinition(GraphQLController).getAnnotation(Controller).getRequiredValue(String) == "/graphql"

        cleanup:
        context.close()
    }

    void "test custom graphql path"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["spec.name": GraphQLConfigurationSpec.simpleName,
                 "graphql.path": "/custom-graphql"]
        ))
        context.start()

        expect:
        context.containsBean(GraphQLExecutionResultHandler)
        context.containsBean(GraphQLInvocation)
        context.containsBean(GraphQLController)
        context.getBeanDefinition(GraphQLController).getAnnotation(Controller).getRequiredValue(String) == "/custom-graphql"
        context.getBean(GraphQLConfiguration).path == "/custom-graphql"

        cleanup:
        context.close()
    }

    void "test graphql disabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["spec.name": GraphQLConfigurationSpec.simpleName,
                 "graphql.enabled": false]
        ))
        context.start()

        expect:
        !context.containsBean(GraphQLExecutionResultHandler)
        !context.containsBean(GraphQLInvocation)
        !context.containsBean(GraphQLController)

        cleanup:
        context.close()
    }

    @Factory
    static class GraphQLFactory {

        @Bean
        @Singleton
        @Requires(property = "graphql.factory", notEquals = StringUtils.FALSE)
        @Requires(property = "spec.name", value = "GraphQLConfigurationSpec")
        GraphQL graphQL() {
            def schema = GraphQLSchema.newSchema().build()
            GraphQL.newGraphQL(schema).build()
        }
    }
}
