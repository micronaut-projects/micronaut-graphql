package io.micronaut.graphql.docs;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class GraphQLDocsTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void graphQlWorksAsExpected() {
        MutableHttpRequest<Object> get = HttpRequest.GET("graphql");
        get.getParameters().add("query", "query { hello }");

        Map retrieve = client.toBlocking().retrieve(get, Map.class);

        assertEquals("Hello World!", ((Map)retrieve.get("data")).get("hello"));
    }
}
