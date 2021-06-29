package io.micronaut.configuration.graphql

import graphql.GraphQL
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.core.util.StringUtils
import jakarta.inject.Singleton

/**
 * @author James Kleeh
 * @since 1.0
 */
@Factory
class GraphQLFactory {

    @Requires(property = "graphql.factory", notEquals = StringUtils.FALSE)
    @Singleton
    GraphQL graphQL() {
        GraphQLObjectType query = new GraphQLObjectType.Builder().name("Query").build()
        GraphQLObjectType mutation = new GraphQLObjectType.Builder().name("Mutation").build()
        GraphQLSchema schema = new GraphQLSchema(query, mutation, Collections.EMPTY_SET)
        new GraphQL(schema)
    }
}
