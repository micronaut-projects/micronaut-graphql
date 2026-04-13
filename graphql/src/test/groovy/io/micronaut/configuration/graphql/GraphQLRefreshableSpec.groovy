/*
 * Copyright 2017-2020 original authors
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

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.Scalars
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.context.scope.Refreshable
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

import static io.micronaut.http.MediaType.APPLICATION_GRAPHQL

class GraphQLRefreshableSpec extends Specification {

    @AutoCleanup
    EmbeddedServer embeddedServer

    RefreshableGraphQLClient graphQLClient

    def setup() {
        RefreshableGraphQLFactory.graphQlVersion.set(0)
        embeddedServer = ApplicationContext.run(
                EmbeddedServer,
                ["spec.name": GraphQLRefreshableSpec.simpleName],
                Environment.TEST)
        graphQLClient = embeddedServer.applicationContext.getBean(RefreshableGraphQLClient)
    }

    void "refreshable GraphQL bean is re-resolved after refresh event"() {
        expect:
        graphQLClient.post("{ foo }").specification.data.foo == "graphQL-1"

        when:
        embeddedServer.applicationContext.publishEvent(new RefreshEvent())

        then:
        graphQLClient.post("{ foo }").specification.data.foo == "graphQL-2"
    }

    @Client("/graphql")
    static interface RefreshableGraphQLClient {

        @Post(produces = APPLICATION_GRAPHQL)
        GraphQLResponseBody post(@Body String body)
    }

    @Factory
    static class RefreshableGraphQLFactory {
        static final AtomicInteger graphQlVersion = new AtomicInteger()

        @Bean
        @Refreshable
        @Requires(property = "spec.name", value = "GraphQLRefreshableSpec")
        GraphQL graphQL() {
            String data = "graphQL-${graphQlVersion.incrementAndGet()}"
            GraphQLSchema schema = GraphQLSchema.newSchema()
                    .query(GraphQLObjectType.newObject()
                            .name("Query")
                            .field {
                                it.name("foo")
                                it.type(Scalars.GraphQLString)
                                it.dataFetcher { data }
                            }
                            .build())
                    .build()
            GraphQL.newGraphQL(schema).build()
        }
    }
}
