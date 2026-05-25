package io.micronaut.configuration.graphql

import graphql.ExecutionInput
import graphql.GraphQLContext
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import jakarta.inject.Singleton
import org.reactivestreams.Publisher

@Singleton
@Primary
@Requires(property = "spec.name", value = "GraphQLControllerSpec")
class SetRequestResponseInputCustomizer implements GraphQLExecutionInputCustomizer {

    @Override
    Publisher<ExecutionInput> customize(ExecutionInput executionInput, HttpRequest httpRequest,
                                        MutableHttpResponse<String> httpResponse) {
        GraphQLContext graphQLContext = executionInput.getGraphQLContext()
        graphQLContext.put("httpRequest", httpRequest)
        graphQLContext.put("httpResponse", httpResponse)
        return Publishers.just(executionInput)
    }
}
