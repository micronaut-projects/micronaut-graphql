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
import graphql.kickstart.tools.SchemaParser;
import graphql.kickstart.tools.SchemaParserBuilder;
import graphql.schema.GraphQLSchema;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

/**
 * @author Marcel Overdijk
 */
@Factory
public class GraphQLFactory {

    @Bean
    @Singleton
    public GraphQL graphQL(ToDoQueryResolver toDoQueryResolver, ToDoMutationResolver toDoMutationResolver,
                           ToDoResolver toDoResolver) {

        // Parse the schema.
        SchemaParserBuilder builder = SchemaParser.newParser()
            .file("schema.graphqls")
            .resolvers(toDoQueryResolver, toDoMutationResolver, toDoResolver);

        // Create the executable schema.
        GraphQLSchema graphQLSchema = builder.build().makeExecutableSchema();

        // Return the GraphQL bean.
        return GraphQL.newGraphQL(graphQLSchema).build();
    }
}
