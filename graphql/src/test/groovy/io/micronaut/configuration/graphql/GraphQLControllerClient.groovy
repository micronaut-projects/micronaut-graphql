package io.micronaut.configuration.graphql

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client

import static io.micronaut.http.MediaType.APPLICATION_GRAPHQL
import static io.micronaut.http.MediaType.APPLICATION_JSON

@Client("/graphql")
interface GraphQLControllerClient {

    @Get("{?query,operationName,variables}")
    GraphQLResponseBody get(@QueryValue String query, @QueryValue @Nullable String operationName, @QueryValue @Nullable String variables)

    @Get(value = "{?query,operationName,variables}", processes = 'application/*')
    HttpResponse<GraphQLResponseBody> getWithResponse(@QueryValue String query, @QueryValue @Nullable String operationName, @QueryValue @Nullable String variables, @Header String accept)

    @Post(value = "{?query,operationName,variables}")
    GraphQLResponseBody post(@QueryValue String query, @QueryValue @Nullable String operationName, @QueryValue @Nullable String variables)

    @Post(produces = APPLICATION_JSON)
    GraphQLResponseBody post(@Body GraphQLRequestBody body)

    @Post(produces = APPLICATION_JSON, processes = 'application/*')
    HttpResponse<GraphQLResponseBody> postJsonWithAccept(@Body GraphQLRequestBody body, @Header String accept)

    @Post(produces = APPLICATION_GRAPHQL)
    GraphQLResponseBody post(@Body String body)

    @Post(produces = APPLICATION_GRAPHQL)
    HttpResponse<GraphQLResponseBody> postWithResponse(@Body String body)

    @Post(consumes = APPLICATION_JSON)
    GraphQLResponseBody postMalformedJson(@Body String body)
}
