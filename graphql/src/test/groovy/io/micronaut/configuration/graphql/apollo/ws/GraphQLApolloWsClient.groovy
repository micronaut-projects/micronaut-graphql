package io.micronaut.configuration.graphql.apollo.ws

import io.micronaut.configuration.graphql.GraphQLJsonSerializer
import io.micronaut.configuration.graphql.GraphQLRequestBody
import io.micronaut.configuration.graphql.GraphQLResponseBody
import io.micronaut.core.annotation.Nullable
import io.micronaut.serde.annotation.Serdeable
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.ClientWebSocket
import io.micronaut.websocket.annotation.OnMessage

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

@ClientWebSocket(uri = "\${graphql.graphql-ws.path:/graphql-ws}", subprotocol = "graphql-ws")
abstract class GraphQLApolloWsClient implements AutoCloseable {

    private BlockingQueue<GraphQLApolloWsResponse> responses = new ArrayBlockingQueue<>(10)
    private final GraphQLJsonSerializer serializer

    GraphQLApolloWsClient(GraphQLJsonSerializer serializer) {
        this.serializer = serializer;
    }

    @OnMessage
    void onMessage(String message, WebSocketSession session) {
        DeserializableResponse response = serializer.deserialize(message, DeserializableResponse)
        responses.add(new GraphQLApolloWsResponse(response.type, response.id, response.payload))
    }

    void send(GraphQLApolloWsRequest request) {
        send(serializer.serialize(new SerializableRequest(request)))
    }

    abstract void send(String message);

    GraphQLApolloWsResponse nextResponse() {
        GraphQLApolloWsResponse response = responses.poll(5, TimeUnit.SECONDS)
        return response
    }
}

class DeserializableResponse {

    GraphQLApolloWsResponse.ServerType type
    String id
    GraphQLResponseBody payload

    void setType(String type) {
        for (GraphQLApolloWsResponse.ServerType serverType : GraphQLApolloWsResponse.ServerType.values()) {
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

@Serdeable
class SerializableRequest {

    String type
    String id
    GraphQLRequestBody payload;

    SerializableRequest(GraphQLApolloWsRequest request){
        this.type = request.getType().getType()
        this.id = request.getId()
        this.payload = request.getPayload()
    }
}
