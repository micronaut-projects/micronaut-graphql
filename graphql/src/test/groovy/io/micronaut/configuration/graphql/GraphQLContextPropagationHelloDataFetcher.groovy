package io.micronaut.configuration.graphql

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import io.micronaut.context.annotation.Requires
import io.micronaut.http.context.ServerRequestContext
import jakarta.inject.Singleton

import java.net.http.HttpRequest

@Singleton
@Requires(property = "spec.name", value = "GraphQLContextPropagationSpec")
class GraphQLContextPropagationHelloDataFetcher implements DataFetcher<String> {

    @Override
    String get(DataFetchingEnvironment env) {
        Optional<HttpRequest> request = ServerRequestContext.currentRequest()
        assert request.isPresent()
        String name = env.getArgument("name")
        if (name == null || name.trim().length() == 0) {
            name = "World"
        }
        String.format("Hello %s!", name)
    }
}
