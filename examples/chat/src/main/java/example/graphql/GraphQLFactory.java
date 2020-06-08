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
package example.graphql;

import graphql.GraphQL;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.io.ResourceResolver;

import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @author Gerard Klijs
 */
@Factory
@SuppressWarnings("Duplicates")
public class GraphQLFactory {

    @Bean
    @Singleton
    public GraphQL graphQL(ResourceResolver resourceResolver,
            MessagesDataFetcher messagesDataFetcher,
            ChatDataFetcher chatDataFetcher,
            StreamDataFetcher streamDataFetcher) {

        SchemaParser schemaParser = new SchemaParser();
        SchemaGenerator schemaGenerator = new SchemaGenerator();

        // Parse the schema.
        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();

        resourceResolver
                .getResourceAsStream("classpath:schema.graphqls")
                .ifPresent(s -> typeRegistry.merge(schemaParser.parse(new BufferedReader(new InputStreamReader(s)))));

        // Create the runtime wiring.
        RuntimeWiring runtimeWiring = RuntimeWiring
                .newRuntimeWiring()
                .scalar(ExtendedScalars.DateTime)
                .type("QueryRoot", typeWiring -> typeWiring
                        .dataFetcher("messages", messagesDataFetcher))
                .type("MutationRoot", typeWiring -> typeWiring
                        .dataFetcher("chat", chatDataFetcher))
                .type("SubscriptionRoot", typeWiring -> typeWiring
                        .dataFetcher("stream", streamDataFetcher))
                .build();

        // Create the executable schema.
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);

        // Return the GraphQL bean.
        return GraphQL.newGraphQL(graphQLSchema).build();
    }
}
