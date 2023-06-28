package io.micronaut.configuration.graphql.ws.apollo

import io.micronaut.configuration.graphql.GraphQLJsonSerializer
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.ClientWebSocket
import io.micronaut.websocket.annotation.OnMessage

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

@ClientWebSocket(uri = "\${graphql.graphql-apollo-ws.path:/graphql-ws}", subprotocol = "graphql-ws")
abstract class GraphQLApolloWsClient implements AutoCloseable {

    private BlockingQueue<GraphQLApolloWsResponse> responses = new ArrayBlockingQueue<>(10)
    private final GraphQLJsonSerializer serializer

    GraphQLApolloWsClient(GraphQLJsonSerializer serializer) {
        this.serializer = serializer;
    }

    @OnMessage
    void onMessage(String message, WebSocketSession session) {
        responses.add(serializer.deserialize(message, GraphQLApolloWsResponse))
    }

    void send(GraphQLApolloWsRequest request) {
        send(serializer.serialize(request))
    }

    abstract void send(String message);

    GraphQLApolloWsResponse nextResponse() {
        return responses.poll(5, TimeUnit.SECONDS)
    }
}
