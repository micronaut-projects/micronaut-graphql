package io.micronaut.configuration.graphql

import graphql.GraphQL
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.core.io.ResourceResolver
import jakarta.inject.Singleton

@Factory
class GraphQLContextPropagationFactory {

    @Bean
    @Singleton
    @Requires(property = "spec.name", value = "GraphQLContextPropagationSpec")
    GraphQL graphQL(ResourceResolver resourceResolver, GraphQLContextPropagationHelloDataFetcher helloDataFetcher) {
        SchemaParser schemaParser = new SchemaParser()
        SchemaGenerator schemaGenerator = new SchemaGenerator()

        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry()
        typeRegistry.merge(schemaParser.parse(new BufferedReader(new InputStreamReader(
                resourceResolver.getResourceAsStream("classpath:schema.graphqls").get()))))

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                        .dataFetcher("hello", helloDataFetcher))
                .build()

        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring)

        GraphQL.newGraphQL(graphQLSchema).build()
    }
}
