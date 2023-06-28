package io.micronaut.configuration.graphql

import graphql.GraphQL
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
        def schema = GraphQLSchema.newSchema().build()
        GraphQL.newGraphQL(schema).build()
    }
}
