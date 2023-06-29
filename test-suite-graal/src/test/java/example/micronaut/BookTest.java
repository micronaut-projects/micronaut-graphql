package example.micronaut;

import example.micronaut.apollo.ws.GraphQLApolloWsClient;
import io.micronaut.configuration.graphql.GraphQLRequestBody;
import io.micronaut.configuration.graphql.GraphQLResponseBody;
import io.micronaut.configuration.graphql.ws.apollo.GraphQLApolloWsRequest;
import io.micronaut.configuration.graphql.ws.apollo.GraphQLApolloWsResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.websocket.WebSocketClient;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void testGraphQLWebSocket() throws InterruptedException {
        WebSocketClient wsClient = embeddedServer.getApplicationContext().createBean(WebSocketClient.class, embeddedServer.getURI());
        GraphQLApolloWsClient graphQLWsClient = Flux.from(wsClient.connect(GraphQLApolloWsClient.class, "/graphql-ws")).blockFirst();

        GraphQLApolloWsRequest request = new GraphQLApolloWsRequest();
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_CONNECTION_INIT.getType());

        graphQLWsClient.send(request);

        GraphQLApolloWsResponse response = graphQLWsClient.nextResponse();
        assertEquals(GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_ACK.getType(), response.getType());

        request = new GraphQLApolloWsRequest();
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_START.getType());
        request.setId("test-id");
        GraphQLRequestBody body = new GraphQLRequestBody();
        body.setQuery("{ bookById(id:\"book-1\") { name, pageCount, author { firstName, lastName} }  }");
        request.setPayload(body);

        graphQLWsClient.send(request);

        response = graphQLWsClient.nextResponse();
        assertEquals(GraphQLApolloWsResponse.ServerType.GQL_DATA.getType(), response.getType());

        GraphQLResponseBody responseBody = response.getPayload();
        assertNotNull(responseBody);
        verifyGraphQLResult(responseBody.getSpecification());

        request = new GraphQLApolloWsRequest();
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_STOP.getType());

        graphQLWsClient.send(request);

        response = graphQLWsClient.nextResponse();
        assertEquals(GraphQLApolloWsResponse.ServerType.GQL_COMPLETE.getType(), response.getType());

        request = new GraphQLApolloWsRequest();
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_CONNECTION_TERMINATE.getType());

        graphQLWsClient.send(request);

        response = graphQLWsClient.nextResponse();
        assertNull(response);
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
