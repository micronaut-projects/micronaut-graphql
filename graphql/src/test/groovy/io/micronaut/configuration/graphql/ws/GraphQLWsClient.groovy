package io.micronaut.configuration.graphql.ws

import io.micronaut.context.annotation.Requires
import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.annotation.ClientWebSocket
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

@Requires(property = "spec.name", value = "GraphQLWsHandlerSpec")
@ClientWebSocket(uri = "\${graphql.graphql-ws.path:/graphql-ws}", subprotocol = "graphql-transport-ws")
abstract class GraphQLWsClient implements AutoCloseable {

    private BlockingQueue<Message> responses = new ArrayBlockingQueue<>(10)

    boolean closed = false;
    CloseReason closeReason = null;

    @OnMessage
    void onMessage(Message message) {
        responses.add(message);
    }

    @OnClose
    void onClose(CloseReason reason) {
        closed = true
        closeReason = reason
    }

    abstract void sendMessage(Message message);

    abstract void sendMap(Map<String, Object> map);

    Message nextResponse() {
        Message response = responses.poll(2, TimeUnit.SECONDS)
        return response
    }
}
