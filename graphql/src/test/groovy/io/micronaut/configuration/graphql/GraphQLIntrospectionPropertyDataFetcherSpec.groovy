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
package io.micronaut.configuration.graphql

import graphql.Scalars
import graphql.GraphQL
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetcherFactoryEnvironment
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton
import spock.lang.AutoCleanup
import spock.lang.Specification

class GraphQLIntrospectionPropertyDataFetcherSpec extends Specification {

    @AutoCleanup
    EmbeddedServer embeddedServer

    GraphQLClient graphQLClient
    GraphQL graphQL

    def setup() {
        embeddedServer = ApplicationContext.run(
                EmbeddedServer,
                ["spec.name": GraphQLIntrospectionPropertyDataFetcherSpec.simpleName],
                Environment.TEST)
        graphQLClient = embeddedServer.applicationContext.getBean(GraphQLClient)
        graphQL = embeddedServer.applicationContext.getBean(GraphQL)
    }

    void "introspected fields are resolved without reflection metadata"() {
        when:
        GraphQLResponseBody response = graphQLClient.book("query { book { name } }")
        def bookType = graphQL.graphQLSchema.getObjectType("Book")
        def nameField = bookType.getFieldDefinition("name")

        then:
        response.getSpecification()["data"]["book"]["name"] == "Micronaut GraphQL"
        graphQL.graphQLSchema.codeRegistry.getDataFetcher(bookType, nameField) instanceof MicronautBeanPropertyDataFetcher
    }

    void "null introspected fields do not fall back to reflection"() {
        when:
        GraphQLResponseBody response = graphQLClient.book("query { nullBook { name } }")
        def bookType = graphQL.graphQLSchema.getObjectType("Book")
        def nameField = bookType.getFieldDefinition("name")

        then:
        response.getSpecification()["data"]["nullBook"]["name"] == null
        graphQL.graphQLSchema.codeRegistry.getDataFetcher(bookType, nameField) instanceof MicronautBeanPropertyDataFetcher
    }

    void "null sources return null"() {
        given:
        def dataFetcher = dataFetcher()

        expect:
        dataFetcher.get(fieldDefinition("name"), null, { null }) == null
    }

    void "map sources preserve null values and missing keys return null"() {
        given:
        def dataFetcher = dataFetcher()

        expect:
        dataFetcher.get(fieldDefinition("name"), [name: null], { null }) == null
        dataFetcher.get(fieldDefinition("name"), [:], { null }) == null
    }

    void "data fetching environment overload falls back to reflective property lookup"() {
        given:
        def dataFetcher = dataFetcher()
        def environment = Stub(DataFetchingEnvironment) {
            getFieldDefinition() >> fieldDefinition("name")
            getSource() >> new LegacyBook("Micronaut GraphQL")
        }

        expect:
        dataFetcher.get(environment) == "Micronaut GraphQL"
    }

    void "missing introspected properties return null"() {
        given:
        def dataFetcher = dataFetcher()

        expect:
        dataFetcher.get(fieldDefinition("name"), new TitleOnlyBook("Micronaut GraphQL"), { null }) == null
    }

    private static GraphQLFieldDefinition fieldDefinition(String name) {
        GraphQLFieldDefinition.newFieldDefinition()
                .name(name)
                .type(Scalars.GraphQLString)
                .build()
    }

    private static MicronautBeanPropertyDataFetcher<Object> dataFetcher() {
        (MicronautBeanPropertyDataFetcher<Object>) MicronautBeanPropertyDataFetcher.factory()
                .get((DataFetcherFactoryEnvironment) null)
    }

    @Client("/graphql")
    static interface GraphQLClient {

        @Get("{?query}")
        GraphQLResponseBody book(@QueryValue String query)
    }

    @Factory
    static class GraphQLFactory {

        @Bean
        @Singleton
        @Requires(property = "spec.name", value = "GraphQLIntrospectionPropertyDataFetcherSpec")
        GraphQL graphQL(BookDataFetcher bookDataFetcher) {
            TypeDefinitionRegistry typeRegistry = new SchemaParser().parse("""
                type Query {
                    book: Book
                    nullBook: Book
                }

                type Book {
                    name: String
                }
            """)

            RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                    .type("Query", typeWiring -> typeWiring
                            .dataFetcher("book", bookDataFetcher)
                            .dataFetcher("nullBook", bookDataFetcher))
                    .build()

            GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring)
            return GraphQL.newGraphQL(graphQLSchema).build()
        }
    }

    @Singleton
    @Requires(property = "spec.name", value = "GraphQLIntrospectionPropertyDataFetcherSpec")
    static class BookDataFetcher implements DataFetcher<Book> {

        @Override
        Book get(graphql.schema.DataFetchingEnvironment environment) {
            return environment.field.name == "nullBook" ? new Book(null) : new Book("Micronaut GraphQL")
        }
    }

    @Introspected(accessKind = Introspected.AccessKind.FIELD)
    static class Book {
        private final String name

        Book(String name) {
            this.name = name
        }
    }

    static class LegacyBook {
        private final String name

        LegacyBook(String name) {
            this.name = name
        }

        String getName() {
            return name
        }
    }

    @Introspected(accessKind = Introspected.AccessKind.FIELD)
    static class TitleOnlyBook {
        private final String title

        TitleOnlyBook(String title) {
            this.title = title
        }
    }
}
