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
import io.micronaut.http.annotation.Controller
import jakarta.inject.Singleton
import spock.lang.Specification

/**
 * @author Marcel Overdijk
 * @since 1.0
 */
class GraphiQLConfigurationSpec extends Specification {

    void "test graphiql disabled by default"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["spec.name": GraphQLConfigurationSpec.simpleName]
        ))
        context.start()

        expect:
        !context.containsBean(GraphiQLController)

        cleanup:
        context.close()
    }

    void "test graphiql enabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["spec.name": GraphQLConfigurationSpec.simpleName,
                 "graphql.graphiql.enabled": true]
        ))
        context.start()

        expect:
        context.containsBean(GraphiQLController)
        context.getBeanDefinition(GraphiQLController).getAnnotation(Controller).getRequiredValue(String) == "/graphiql"

        cleanup:
        context.close()
    }

    void "test custom graphiql version"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["spec.name": GraphQLConfigurationSpec.simpleName,
                 "graphql.graphiql.enabled": true,
                 "graphql.graphiql.version"   : "0.13.1"]
        ))
        context.start()

        expect:
        context.containsBean(GraphiQLController)
        context.getBean(GraphQLConfiguration).graphiql.version == "0.13.1"

        cleanup:
        context.close()
    }

    void "test custom graphiql path"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["spec.name": GraphQLConfigurationSpec.simpleName,
                 "graphql.graphiql.enabled": true,
                 "graphql.graphiql.path"   : "/custom-graphiql"]
        ))
        context.start()

        expect:
        context.containsBean(GraphiQLController)
        context.getBeanDefinition(GraphiQLController).getAnnotation(Controller).getRequiredValue(String) == "/custom-graphiql"
        context.getBean(GraphQLConfiguration).graphiql.path == "/custom-graphiql"

        cleanup:
        context.close()
    }

    void "test graphiql disabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["spec.name": GraphQLConfigurationSpec.simpleName,
                 "graphql.graphiql.enabled": false]
        ))
        context.start()

        expect:
        !context.containsBean(GraphiQLController)

        cleanup:
        context.close()
    }

    @Factory
    static class GraphQLFactory {

        @Bean
        @Singleton
        @Requires(property = "spec.name", value = "GraphiQLConfigurationSpec")
        GraphQL graphQL() {
            def schema = GraphQLSchema.newSchema().build()
            GraphQL.newGraphQL(schema).build()
        }
    }
}
