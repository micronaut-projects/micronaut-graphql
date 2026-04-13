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

import com.apollographql.federation.graphqljava.Federation
import graphql.GraphQL
import graphql.schema.DataFetcher
import graphql.schema.GraphQLSchema
import graphql.schema.TypeResolver
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton
import spock.lang.AutoCleanup
import spock.lang.Specification

import static io.micronaut.http.MediaType.APPLICATION_JSON

class GraphQLFederationSpec extends Specification {

    @AutoCleanup
    EmbeddedServer embeddedServer

    GraphQLClient graphQLClient

    def setup() {
        embeddedServer = ApplicationContext.run(
                EmbeddedServer,
                ["spec.name": GraphQLFederationSpec.simpleName],
                Environment.TEST)
        graphQLClient = embeddedServer.applicationContext.getBean(GraphQLClient)
    }

    void "apollo federation exposes _service sdl"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody()
        body.query = "query { _service { sdl } }"

        when:
        GraphQLResponseBody response = graphQLClient.post(body)

        then:
        String sdl = response.specification.data._service.sdl
        sdl.contains('https://specs.apollo.dev/federation/v2.0')
        sdl.contains('type Product @key(fields : "upc"')
    }

    void "apollo federation resolves _entities representations"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody()
        body.query = """
            query(\$representations: [_Any!]!) {
                _entities(representations: \$representations) {
                    ... on Product {
                        upc
                        name
                    }
                }
            }
            """
        body.variables = [
                representations: [
                        [__typename: "Product", upc: "top-1"]
                ]
        ]

        when:
        GraphQLResponseBody response = graphQLClient.post(body)

        then:
        response.specification.data._entities == [[upc: "top-1", name: "Table"]]
    }

    @Client("/graphql")
    static interface GraphQLClient {

        @Post(produces = APPLICATION_JSON)
        GraphQLResponseBody post(@Body GraphQLRequestBody body)
    }

    @Factory
    static class GraphQLFactory {

        private static final Map<String, Product> PRODUCTS = [
                "top-1": new Product("top-1", "Table")
        ]

        @Bean
        @Singleton
        @Requires(property = "spec.name", value = "GraphQLFederationSpec")
        GraphQL graphQL() {
            TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry()
            typeRegistry.merge(new SchemaParser().parse("""
                extend schema
                    @link(url: "https://specs.apollo.dev/federation/v2.0", import: ["@key"])

                type Query {
                    topProduct: Product
                }

                type Product @key(fields: "upc") {
                    upc: ID!
                    name: String
                }
                """))

            RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                    .type("Query", { typeWiring ->
                        typeWiring.dataFetcher("topProduct", ({ environment -> PRODUCTS["top-1"] } as DataFetcher<Product>))
                    })
                    .build()

            GraphQLSchema graphQLSchema = Federation.transform(typeRegistry, runtimeWiring)
                    .fetchEntities({ environment ->
                        environment.getArgument("representations").collect({ representation -> PRODUCTS[representation["upc"]] })
                    } as DataFetcher<List<Product>>)
                    .resolveEntityType({ environment -> environment.schema.getObjectType("Product") } as TypeResolver)
                    .build()

            return GraphQL.newGraphQL(graphQLSchema).build()
        }
    }

    static class Product {
        final String upc
        final String name

        Product(String upc, String name) {
            this.upc = upc
            this.name = name
        }
    }
}
