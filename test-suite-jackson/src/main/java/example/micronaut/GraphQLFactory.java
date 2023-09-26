package example.micronaut;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.io.ResourceResolver;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

@Requires(property= "spec.name", value = "BookTest")
@Factory
public class GraphQLFactory {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQLFactory.class);

    @Bean
    @Singleton
    public GraphQL graphQL(ResourceResolver resourceResolver, GraphQLDataFetchers graphQLDataFetchers) {
        SchemaParser schemaParser = new SchemaParser();
        SchemaGenerator schemaGenerator = new SchemaGenerator();

        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
        Optional<InputStream> graphqlSchema = resourceResolver.getResourceAsStream("classpath:schema.graphqls");

        if (graphqlSchema.isPresent()) {
            typeRegistry.merge(schemaParser.parse(new BufferedReader(new InputStreamReader(graphqlSchema.get()))));

            RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                    .dataFetcher("books", graphQLDataFetchers.getBookDataFetcher())
                )
                .build();

            GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);

            return GraphQL.newGraphQL(graphQLSchema).build();
        } else {
            LOG.debug("No GraphQL services found, returning empty schema");
            return new GraphQL.Builder(GraphQLSchema.newSchema().build()).build();
        }
    }
}
