package io.micronaut.configuration.graphql

import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.ClientWebSocket
import io.micronaut.websocket.annotation.OnMessage

import javax.annotation.Nullable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

@ClientWebSocket(uri = "/graphql-ws", subprotocol = "graphql-ws")
abstract class GraphQLWsClient implements AutoCloseable {

    private BlockingQueue<GraphQLWsResponse> responses = new ArrayBlockingQueue<>(10)
    private final GraphQLJsonSerializer serializer

    GraphQLWsClient(GraphQLJsonSerializer serializer) {
        this.serializer = serializer;
    }

    @OnMessage
    void onMessage(String message, WebSocketSession session) {
        DeserializableResponse response = serializer.deserialize(message, DeserializableResponse)
        responses.add(new GraphQLWsResponse(response.type, response.id, response.payload))
    }

    void send(GraphQLWsRequest request) {
        send(serializer.serialize(new SerializableRequest(request)))
    }

    abstract void send(String message);

    GraphQLWsResponse nextResponse() {
        GraphQLWsResponse response = responses.poll(10, TimeUnit.SECONDS)
        assert response != null: "Did not receive next message within 10 seconds"
        return response
    }
}

class DeserializableResponse {

    GraphQLWsResponse.ServerType type
    String id
    GraphQLResponseBody payload

    void setType(String type) {
        for (GraphQLWsResponse.ServerType serverType : GraphQLWsResponse.ServerType.values()) {
            if (serverType.getType().equals(type)) {
                this.type = serverType
            }
        }
    }

    void setId(@Nullable String id) {
        this.id = id
    }

    void setPayload(@Nullable GraphQLResponseBody payload) {
        this.payload = payload
    }
}

class SerializableRequest {

    String type
    String id
    GraphQLRequestBody payload;

    SerializableRequest(GraphQLWsRequest request){
        this.type = request.getType().getType()
        this.id = request.getId()
        this.payload = request.getPayload()
    }
}
