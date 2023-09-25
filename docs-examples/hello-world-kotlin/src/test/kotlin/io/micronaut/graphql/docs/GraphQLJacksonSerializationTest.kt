package io.micronaut.graphql.docs

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.configuration.graphql.GraphQLResponseBody

@MicronautTest
class GraphQLJacksonSerializationTest {

    @Inject
    lateinit var mapper: ObjectMapper

    @Test
    fun serializeGraphQLResponseBody() {
        val specification = mapOf("foo" to "bar")
        val response = GraphQLResponseBody(specification)
        val expected = """{"foo":"bar"}"""
        val m = ObjectMapper()
        Assertions.assertEquals(expected, m.writeValueAsString(response))
        Assertions.assertEquals(expected, mapper.writeValueAsString(response))
    }
}
