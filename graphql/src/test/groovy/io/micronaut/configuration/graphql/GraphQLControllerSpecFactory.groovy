package io.micronaut.configuration.graphql

import graphql.GraphQL
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton

@Factory
class GraphQLControllerSpecFactory {

    static GraphQL graphQL

    @Bean
    @Singleton
    @Requires(property = "spec.name", value = "GraphQLControllerSpec")
    GraphQL graphQL() {
        assert graphQL != null
        graphQL
    }
}
