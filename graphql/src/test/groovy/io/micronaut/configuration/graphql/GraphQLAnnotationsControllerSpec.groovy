/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.micronaut.configuration.graphql.runtime.GraphQLRuntimeWiringBuilderCustomizer
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.core.io.ResourceResolver
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.Specification

import javax.inject.Singleton

/**
 * @author Denis Stepanov
 */
class GraphQLAnnotationsControllerSpec extends Specification {

    @AutoCleanup
    EmbeddedServer embeddedServer

    GraphQLClient graphQLClient

    def setup() {
        embeddedServer = ApplicationContext.run(
                EmbeddedServer,
                [
                        "spec.name"      : GraphQLAnnotationsControllerSpec.simpleName,
                        'graphql.factory': 'false',
                        'graphql.enabled': 'true',
                ],
                Environment.TEST)
        graphQLClient = embeddedServer.applicationContext.getBean(GraphQLClient)
    }

    void "test hello query data fetcher"() {
        given:
            String query = "{ hello }"

        when:
            GraphQLResponseBody response = graphQLClient.get(query)

        then:
            response.specification.data.hello == "Hello world"
    }

    void "test type resolver"() {
        given:
            String query = "query { favoriteAnimal { name } }"

        when:
            GraphQLResponseBody response = graphQLClient.get(query)

        then:
            response.specification.data.favoriteAnimal.name == "Snow"
    }

    void "test type field fetchers with different arguments"() {
        given:
            String query = """query { dog { internalClassName } favoriteAnimal { name internalClassName prefixName(prefix: "Mr.") alias: prefixName(prefix: "Mr.") } }"""

        when:
            GraphQLResponseBody response = graphQLClient.get(query)

        then:
            response.specification.data.favoriteAnimal.name == "Snow"
            response.specification.data.favoriteAnimal.prefixName == "Mr.Snow"
            response.specification.data.favoriteAnimal.alias == "Mr.Snow"
            response.specification.data.favoriteAnimal.internalClassName == "Cat"
            response.specification.data.dog.internalClassName == "Dog"
    }

    void "test native data fetcher"() {
        given:
            String query = """query { weather }"""

        when:
            GraphQLResponseBody response = graphQLClient.get(query)

        then:
            response.specification.data.weather == "Sunny"
    }

    void "test custom field name"() {
        given:
            String query = """query { randomNumber }"""

        when:
            GraphQLResponseBody response = graphQLClient.get(query)

        then:
            response.specification.data.randomNumber == 42
    }

    void "test mutation"() {
        given:
            String query = """mutation { createDog(name: "Ugly") { name } }"""

        when:
            GraphQLResponseBody response = graphQLClient.get(query)

        then:
            response.specification.data.createDog.name == "Ugly"
    }

    @Client("/graphql")
    static interface GraphQLClient {

        @Get("{?query}")
        GraphQLResponseBody get(@QueryValue String query)
    }

    @Factory
    static class GraphQLFactory {

        @Bean
        @Singleton
        @Requires(property = "spec.name", value = "GraphQLAnnotationsControllerSpec")
        GraphQL graphQL(ResourceResolver resourceResolver, GraphQLRuntimeWiringBuilderCustomizer runtimeWiringBuilderCustomizer) {
            SchemaParser schemaParser = new SchemaParser()
            SchemaGenerator schemaGenerator = new SchemaGenerator()

            TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry()
            typeRegistry.merge(schemaParser.parse(new BufferedReader(new InputStreamReader(
                    resourceResolver.getResourceAsStream("classpath:example1-schema.graphqls").get()))))

            RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring()

            runtimeWiringBuilderCustomizer.accept(runtimeWiringBuilder)

            RuntimeWiring runtimeWiring = runtimeWiringBuilder.build()

            GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring)

            new GraphQL(graphQLSchema)
        }
    }
}
