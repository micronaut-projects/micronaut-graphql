package example.micronaut;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "spec.name", value = "BookTest")
@MicronautTest
class BookTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    EmbeddedServer embeddedServer;

    @Test
    void testGraphQL() {
        MutableHttpRequest<Object> request = HttpRequest.POST("graphql", "{\"query\":\"query { books { name, pageCount } }\"}");
        Map result = client.toBlocking().retrieve(request, Map.class);

        assertNotNull(result);
        assertTrue(result.containsKey("data"));
    }
}
