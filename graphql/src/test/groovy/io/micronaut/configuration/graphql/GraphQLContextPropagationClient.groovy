package io.micronaut.configuration.graphql

import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client

@Client("/graphql")
interface GraphQLContextPropagationClient {

    @Get("{?query}")
    GraphQLResponseBody hello(@QueryValue String query)
}
