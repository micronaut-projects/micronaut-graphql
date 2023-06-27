package io.micronaut.configuration.graphql.ws

import graphql.ExecutionInput
import io.micronaut.configuration.graphql.GraphQLExecutionInputCustomizer
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.websocket.WebSocketClient
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import spock.lang.AutoCleanup
import spock.lang.Specification

class GraphQLWsHandlerSpec extends Specification {

    @AutoCleanup
    EmbeddedServer embeddedServer

    GraphQLWsClient graphQLWsClient

    def setup() {
        embeddedServer = ApplicationContext.run(EmbeddedServer, ["spec.name": GraphQLWsHandlerSpec.simpleName], "websocket") as EmbeddedServer
        WebSocketClient wsClient = embeddedServer.applicationContext.createBean(WebSocketClient, embeddedServer.getURI())
        graphQLWsClient = Flux.from(wsClient.connect(GraphQLWsClient, "/graphql-ws")).blockFirst()
    }

    void "connection initialization"() {
        given:
        Message request = new ConnectionInitMessage();

        when: "a connection is initialized"
        graphQLWsClient.send(request)
        Message ack = graphQLWsClient.nextResponse()

        then: "the server responds with an ack"
        ack instanceof ConnectionAckMessage
    }

    void "connection initialization timeout"() {
        when: "no connection is initialized before the configured timeout"
        Thread.sleep(6000)

        then: "the socket is closed with an error response"
        graphQLWsClient.nextResponse() == null
        graphQLWsClient.closed
        graphQLWsClient.closeReason.getCode() == 4408
        graphQLWsClient.closeReason.getReason() == "Connection initialisation timeout."
    }

    void "client sends multiple connection requests"() {
        given:
        Message request1 = new ConnectionInitMessage()
        Message request2 = new ConnectionInitMessage()

        when: "a connection init is received on an already established connection"
        graphQLWsClient.send(request1)
        graphQLWsClient.send(request2)
        Message ack = graphQLWsClient.nextResponse()

        then: "the socket is closed with an error response"
        ack instanceof ConnectionAckMessage
        graphQLWsClient.nextResponse() == null
        graphQLWsClient.closed
        graphQLWsClient.closeReason.getCode() == 4403
        graphQLWsClient.closeReason.getReason() == "Too many initialisation requests."
    }

    void "client sends heartbeat" () {
        given:
        Message connect = new ConnectionInitMessage()
        Message ping = new PingMessage()

        when:
        graphQLWsClient.send(connect)
        Message ack = graphQLWsClient.nextResponse()

        then:
        ack instanceof ConnectionAckMessage

        when: "a heartbeat ping message is received"
        graphQLWsClient.send(ping)
        Message pong = graphQLWsClient.nextResponse()

        then: "a heartbeat pong message is sent in response"
        pong instanceof PongMessage
    }

    void "client subscribes to a single-result query operation and receives result" () {
        given:
        Message connect = new ConnectionInitMessage()
        Message subscribe = new SubscribeMessage("query_id", new SubscribeMessage.SubscribePayload("query{ foo }"));

        when:
        graphQLWsClient.send(connect)
        graphQLWsClient.nextResponse()
        graphQLWsClient.send(subscribe)
        Message nextResult = graphQLWsClient.nextResponse()
        Message completeResult = graphQLWsClient.nextResponse()

        then:
        nextResult != null && nextResult instanceof NextMessage && nextResult.getPayload().get("data") == [foo: "bar"]
        completeResult != null && completeResult instanceof CompleteMessage && "query_id" == completeResult.getId()
    }

    void "client subscribes to a mutation operation and receives result" () {
        given:
        Message connect = new ConnectionInitMessage()
        Message subscribe = new SubscribeMessage("change_id", new SubscribeMessage.SubscribePayload("mutation{ change( newValue: \"Value_B\" ){ current old }}"));

        when:
        graphQLWsClient.send(connect)
        graphQLWsClient.nextResponse()
        graphQLWsClient.send(subscribe)
        Message nextResult = graphQLWsClient.nextResponse()
        Message completeResult = graphQLWsClient.nextResponse()

        then:
        nextResult != null && nextResult instanceof NextMessage && nextResult.getPayload().get("data") == [change: [current: "Value_B", old: ["Value_A"]]]
        completeResult != null && completeResult instanceof CompleteMessage && "change_id" == completeResult.getId()
    }

    void "client subscribes to a mutation operation that uses a customizer that evaluates the http request and receives result" () {
        given:
        Message connect = new ConnectionInitMessage()
        Message subscribe = new SubscribeMessage("change_id", new SubscribeMessage.SubscribePayload("mutation{ change( newValue: \"\$[path]\" ){ current old }}"));

        when:
        graphQLWsClient.send(connect)
        graphQLWsClient.nextResponse()
        graphQLWsClient.send(subscribe)
        Message nextResult = graphQLWsClient.nextResponse()
        Message completeResult = graphQLWsClient.nextResponse()

        then:
        nextResult != null && nextResult instanceof NextMessage && nextResult.getPayload().get("data") == [change: [current: "/graphql-ws", old: ["Value_A"]]]
        completeResult != null && completeResult instanceof CompleteMessage && "change_id" == completeResult.getId()
    }

    void "client subscribes to an operation with invalid query and receives error result" () {
        given:
        Message connect = new ConnectionInitMessage()
        Message subscribe = new SubscribeMessage("query_id", new SubscribeMessage.SubscribePayload("foo"));

        when:
        graphQLWsClient.send(connect)
        graphQLWsClient.nextResponse()
        graphQLWsClient.send(subscribe)
        Message errorResult = graphQLWsClient.nextResponse()

        then:
        errorResult != null && errorResult instanceof ErrorMessage && errorResult.getPayload() != null && !errorResult.getPayload().isEmpty() && "query_id" == errorResult.getId()
        graphQLWsClient.nextResponse() == null
    }

    void "client subscribes to a long-running subscription operation and receive all results" () {
        given:
        Message connect = new ConnectionInitMessage()
        Message subscribe = new SubscribeMessage("counter_id", new SubscribeMessage.SubscribePayload("subscription{ counter }"));

        when:
        graphQLWsClient.send(connect)
        graphQLWsClient.nextResponse()
        graphQLWsClient.send(subscribe)
        Message nextResult1 = graphQLWsClient.nextResponse()
        Message nextResult2 = graphQLWsClient.nextResponse()
        Message nextResult3 = graphQLWsClient.nextResponse()
        Message completeResult = graphQLWsClient.nextResponse()

        then:
        nextResult1 != null && nextResult1 instanceof NextMessage && nextResult1.getPayload().get("data") == [counter: 0]
        nextResult2 != null && nextResult2 instanceof NextMessage && nextResult2.getPayload().get("data") == [counter: 1]
        nextResult3 != null && nextResult3 instanceof NextMessage && nextResult3.getPayload().get("data") == [counter: 2]
        completeResult != null && completeResult instanceof CompleteMessage && "counter_id" == completeResult.getId()
    }

    void "client subscribes to a long-running subscription operation and completes after 1 result" () {
        given:
        Message connect = new ConnectionInitMessage()
        Message subscribe = new SubscribeMessage("counter_id", new SubscribeMessage.SubscribePayload("subscription{ counter }"));

        when:
        graphQLWsClient.send(connect)
        graphQLWsClient.nextResponse()
        graphQLWsClient.send(subscribe)
        Message nextResult1 = graphQLWsClient.nextResponse()
        graphQLWsClient.send(new CompleteMessage("counter_id"))
        List<Message> responses = new ArrayList<>()
        while(true) {
            Message next = graphQLWsClient.nextResponse()
            if (next == null) {
                break
            }
            responses.add(next)
        }

        then:
        nextResult1 != null && nextResult1 instanceof NextMessage && nextResult1.getPayload().get("data") == [counter: 0]
        responses.stream().noneMatch(message -> message instanceof CompleteMessage)
    }

    void "reject subscription without a connection" () {
        given:
        Message subscribe = new SubscribeMessage("12345", new SubscribeMessage.SubscribePayload("foo"))

        when: "a subscribe message is received without a connection"
        graphQLWsClient.send(subscribe)

        then: "the socket is closed with a an unauthorized error"
        graphQLWsClient.nextResponse() == null
        graphQLWsClient.closed
        graphQLWsClient.closeReason.getCode() == 4401
        graphQLWsClient.closeReason.getReason() == "Unauthorized."
    }

    void "reject duplicate subscriptions" () {
        given:
        Message connect = new ConnectionInitMessage()
        Message subscribe1 = new SubscribeMessage("counter_id", new SubscribeMessage.SubscribePayload("subscription{ counter }"));
        Message subscribe2 = new SubscribeMessage("counter_id", new SubscribeMessage.SubscribePayload("subscription{ counter }"));

        when:
        graphQLWsClient.send(connect)
        graphQLWsClient.nextResponse()
        graphQLWsClient.send(subscribe1)
        graphQLWsClient.send(subscribe2)
        Message response = graphQLWsClient.nextResponse()
        while(response != null) {
            response = graphQLWsClient.nextResponse()
        }

        then: "the socket is closed with a subscriber already exists error"
        graphQLWsClient.closed
        graphQLWsClient.closeReason.getCode() == 4409
        graphQLWsClient.closeReason.getReason() == "Subscriber for counter_id already exists."
    }

    void "reject invalid message" () {
        given:
        Map<String, Object> invalidMessage = Map.of("foo", "bar")

        when:
        graphQLWsClient.send(invalidMessage)
        graphQLWsClient.nextResponse()

        then:
        graphQLWsClient.closed
        graphQLWsClient.closeReason.getCode() == 4400
        graphQLWsClient.closeReason.getReason() == "Invalid message."

    }

    void "reject invalid type message" () {
        given:
        Map<String, Object> invalidMessage = Map.of("type", "foo", "id", "12345")

        when:
        graphQLWsClient.send(invalidMessage)
        graphQLWsClient.nextResponse()

        then:
        graphQLWsClient.closed
        graphQLWsClient.closeReason.getCode() == 4400
        graphQLWsClient.closeReason.getReason() == "Invalid message."

    }

}

@Singleton
@Primary
@Requires(property = "spec.name", value = "GraphQLWsHandlerSpec")
class SetValueFromRequestInputCustomizer implements GraphQLExecutionInputCustomizer {
    private final static String PATH_PLACEHOLDER = "\$[path]"

    @Override
    Publisher<ExecutionInput> customize(ExecutionInput executionInput, HttpRequest httpRequest,
                                        MutableHttpResponse<String> httpResponse) {
        if (executionInput.getQuery().contains(PATH_PLACEHOLDER)) {
            return Publishers.just(executionInput.transform({
                builder -> builder.query(executionInput.getQuery().replace(PATH_PLACEHOLDER, httpRequest.getPath()))
            }))
        } else {
            return Publishers.just(executionInput)
        }
    }
}
