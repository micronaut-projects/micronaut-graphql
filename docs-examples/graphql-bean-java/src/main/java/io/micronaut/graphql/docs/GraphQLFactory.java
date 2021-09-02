/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.graphql.docs;

// tag::imports[]

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.io.ResourceResolver;
import jakarta.inject.Singleton;

import java.io.BufferedReader;
import java.io.InputStreamReader;
// end::imports[]

/**
 * @author Michael Ortali
 */
// tag::clazz[]
@Factory
public class GraphQLFactory {
    @Bean
    @Singleton
    public GraphQL graphQL(ResourceResolver resourceResolver) {
        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
        typeRegistry.merge(new SchemaParser().parse(new BufferedReader(new InputStreamReader(
                resourceResolver.getResourceAsStream("classpath:schema.graphqls").get()))));

        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(
                typeRegistry,
                RuntimeWiring.newRuntimeWiring()
                        .type("Query", typeWiring -> typeWiring.dataFetcher(
                                "hello",
                                env -> "Hello " + env.getArgumentOrDefault("name", "World") + "!"))
                        .build());

        return GraphQL.newGraphQL(graphQLSchema).build();
    }
}
// end::clazz[]
