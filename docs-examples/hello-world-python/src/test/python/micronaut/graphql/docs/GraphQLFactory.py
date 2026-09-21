# tag::imports[]
from graphql import GraphQL
from graphql.schema import GraphQLSchema
from graphql.schema.idl import RuntimeWiring, SchemaGenerator, SchemaParser, TypeDefinitionRegistry
from jakarta.inject import Singleton
from java.io import BufferedReader, InputStreamReader
from micronaut.context.annotation import Bean, Factory
from micronaut.core.io import ResourceResolver

from .HelloDataFetcher import HelloDataFetcher
# end::imports[]


# tag::clazz[]
@Factory  # <1>
class GraphQLFactory:

    @Bean
    @Singleton
    def graph_ql(self, resource_resolver: ResourceResolver, hello_data_fetcher: HelloDataFetcher) -> GraphQL:  # <2>

        schema_parser = SchemaParser()
        schema_generator = SchemaGenerator()

        # Parse the schema.
        type_registry = TypeDefinitionRegistry()
        type_registry.merge(schema_parser.parse(BufferedReader(InputStreamReader(
            resource_resolver.getResourceAsStream("classpath:schema.graphqls").get()))))

        # Create the runtime wiring.
        runtime_wiring = (RuntimeWiring.newRuntimeWiring()
                          .type("Query", lambda type_wiring: type_wiring
                                .dataFetcher("hello", hello_data_fetcher))
                          .build())

        # Create the executable schema.
        graphql_schema: GraphQLSchema = schema_generator.makeExecutableSchema(type_registry, runtime_wiring)

        # Return the GraphQL bean.
        return GraphQL.newGraphQL(graphql_schema).build()
# end::clazz[]
