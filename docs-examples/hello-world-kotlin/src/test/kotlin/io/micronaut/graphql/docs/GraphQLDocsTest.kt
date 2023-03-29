package io.micronaut.graphql.docs

import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@MicronautTest
class GraphQLDocsTest {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun graphQlWorksAsExpected() {
        val get = HttpRequest.GET<Any>("graphql").apply {
            parameters.add("query", "query { hello }")
        }

        val retrieve = client.toBlocking().retrieve(get, Map::class.java)

        Assertions.assertEquals(
            "Hello World!",
            (retrieve["data"] as Map<*, *>)["hello"]
        )
    }
}
