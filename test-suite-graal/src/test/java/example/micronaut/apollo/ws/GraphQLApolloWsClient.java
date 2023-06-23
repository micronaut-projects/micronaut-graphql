package example.micronaut.apollo.ws;

import io.micronaut.configuration.graphql.GraphQLJsonSerializer;
import io.micronaut.configuration.graphql.apollo.ws.GraphQLApolloWsRequest;
import io.micronaut.configuration.graphql.apollo.ws.GraphQLApolloWsResponse;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.ClientWebSocket;
import io.micronaut.websocket.annotation.OnMessage;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@ClientWebSocket(uri = "${graphql.graphql-apollo-ws.path:/graphql-ws}", subprotocol = "graphql-apollo-ws")
public abstract class GraphQLApolloWsClient implements AutoCloseable {

    private BlockingQueue<GraphQLApolloWsResponse> responses = new ArrayBlockingQueue<>(10);
    private final GraphQLJsonSerializer serializer;

    GraphQLApolloWsClient(GraphQLJsonSerializer serializer) {
        this.serializer = serializer;
    }

    @OnMessage
    void onMessage(String message, WebSocketSession session) {
        GraphQLApolloWsResponse response = serializer.deserialize(message, GraphQLApolloWsResponse.class);
        responses.add(response);
    }

    public void send(GraphQLApolloWsRequest request) {
        String message = serializer.serialize(request);
        send(message);
    }

    abstract void send(String message);

    public GraphQLApolloWsResponse nextResponse() throws InterruptedException {
        return responses.poll(5, TimeUnit.SECONDS);
    }
}
