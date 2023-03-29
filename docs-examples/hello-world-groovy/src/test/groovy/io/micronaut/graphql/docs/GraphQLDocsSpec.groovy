package io.micronaut.graphql.docs

import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class GraphQLDocsSpec extends Specification {

    @Client("/")
    @Inject
    HttpClient client

    def "graphql works as expected"() {
        when:
        def response = client.toBlocking().retrieve(HttpRequest.GET("graphql").tap { parameters.add("query", "query { hello }") }, Map)

        then:
        response.data.hello == "Hello World!"
    }
}
