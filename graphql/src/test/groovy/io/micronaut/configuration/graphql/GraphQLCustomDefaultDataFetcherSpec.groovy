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

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.schema.DataFetcher
import graphql.schema.DataFetcherFactory
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
import jakarta.inject.Singleton
import spock.lang.AutoCleanup
import spock.lang.Specification

class GraphQLCustomDefaultDataFetcherSpec extends Specification {

    @AutoCleanup
    ApplicationContext applicationContext = ApplicationContext.run(
            ["spec.name": GraphQLCustomDefaultDataFetcherSpec.simpleName],
            Environment.TEST)

    void "custom default data fetcher is preserved"() {
        given:
        GraphQL graphQL = applicationContext.getBean(GraphQL)

        when:
        def result = graphQL.execute(ExecutionInput.newExecutionInput("query { book { name } }").build())
        def bookType = graphQL.graphQLSchema.getObjectType("Book")
        def nameField = bookType.getFieldDefinition("name")

        then:
        result.toSpecification()["data"]["book"]["name"] == "custom default"
        graphQL.graphQLSchema.codeRegistry.getDataFetcher(bookType, nameField) instanceof CustomDefaultDataFetcher
    }

    @Factory
    static class GraphQLFactory {

        @Bean
        @Singleton
        @Requires(property = "spec.name", value = "GraphQLCustomDefaultDataFetcherSpec")
        GraphQL graphQL(BookDataFetcher bookDataFetcher) {
            TypeDefinitionRegistry typeRegistry = new SchemaParser().parse("""
                type Query {
                    book: Book
                }

                type Book {
                    name: String
                }
            """)

            RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                    .type("Query", typeWiring -> typeWiring.dataFetcher("book", bookDataFetcher))
                    .build()

            GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring)
            graphQLSchema = graphQLSchema.transform(builder -> builder.codeRegistry(
                    graphQLSchema.codeRegistry.transform(codeRegistryBuilder ->
                            codeRegistryBuilder.defaultDataFetcher(CustomDefaultDataFetcher.factory()))
            ))
            return GraphQL.newGraphQL(graphQLSchema).build()
        }
    }

    @Singleton
    @Requires(property = "spec.name", value = "GraphQLCustomDefaultDataFetcherSpec")
    static class BookDataFetcher implements DataFetcher<Book> {

        @Override
        Book get(graphql.schema.DataFetchingEnvironment environment) {
            return new Book("Micronaut GraphQL")
        }
    }

    static class CustomDefaultDataFetcher implements DataFetcher<Object> {
        private static final CustomDefaultDataFetcher INSTANCE = new CustomDefaultDataFetcher()
        private static final DataFetcherFactory<?> FACTORY = environment -> INSTANCE

        static DataFetcherFactory<?> factory() {
            return FACTORY
        }

        @Override
        Object get(graphql.schema.DataFetchingEnvironment environment) {
            return "custom default"
        }
    }

    static class Book {
        final String name

        Book(String name) {
            this.name = name
        }
    }
}
