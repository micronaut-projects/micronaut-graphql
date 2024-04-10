package example.micronaut;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
public class BookTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    EmbeddedServer embeddedServer;

    @Test
    void testGraphQL() {
        MutableHttpRequest<Object> get = HttpRequest.GET("graphql");
        get.getParameters().add("query", "{ bookById(id:\"book-1\") { name, pageCount, author { firstName, lastName} }  }");

        Map result = client.toBlocking().retrieve(get, Map.class);

        verifyGraphQLResult(result);
    }

    private void verifyGraphQLResult(Map result) {
        assertNotNull(result);

        Map dataMap = (Map)result.get("data");
        assertNotNull(dataMap);
        assertEquals(1, dataMap.size());

        Map bookByIdMap = (Map)dataMap.get("bookById");
        assertNotNull(bookByIdMap);
        assertEquals(3, bookByIdMap.size());
        assertEquals("Harry Potter and the Philosopher's Stone", bookByIdMap.get("name"));
        assertEquals(223, bookByIdMap.get("pageCount"));

        Map authorMap = (Map)bookByIdMap.get("author");
        assertNotNull(authorMap);
        assertEquals(2, authorMap.size());
        assertEquals("Joanne", authorMap.get("firstName"));
        assertEquals("Rowling", authorMap.get("lastName"));
    }
}
