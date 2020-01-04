package io.micronaut.configuration.graphql

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.websocket.RxWebSocketClient
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * @author Gerard Klijs
 * @since 1.3
 */
class GraphQLWsKeepAliveSpec extends Specification {

    @AutoCleanup
    EmbeddedServer embeddedServer

    GraphQLWsClient graphQLWsClient

    def setup() {
        embeddedServer = ApplicationContext.run(
                EmbeddedServer, "keepalive") as EmbeddedServer
        RxWebSocketClient wsClient = embeddedServer.applicationContext.createBean(RxWebSocketClient, embeddedServer.getURI())
        graphQLWsClient = wsClient.connect(GraphQLWsClient, "/ka-ws").blockingFirst();
    }

    void "test keep alive starts and stops"() {
        given:
        GraphQLWsRequest request = new GraphQLWsRequest()
        request.setType(GraphQLWsRequest.ClientType.GQL_CONNECTION_INIT.getType())

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLWsResponse response = graphQLWsClient.nextResponse()
        response.getType() == GraphQLWsResponse.ServerType.GQL_CONNECTION_ACK.getType()
        GraphQLWsResponse firstAk = graphQLWsClient.nextResponse()
        firstAk != null
        GraphQLWsResponse secondAk = graphQLWsClient.nextResponse()
        secondAk != null
        request.setType(GraphQLWsRequest.ClientType.GQL_CONNECTION_TERMINATE.getType())
        graphQLWsClient.send(request)
        GraphQLWsResponse noResponse = graphQLWsClient.nextResponse()
        noResponse == null

        and:
        firstAk.type == GraphQLWsResponse.ServerType.GQL_CONNECTION_KEEP_ALIVE.getType()
        secondAk.type == GraphQLWsResponse.ServerType.GQL_CONNECTION_KEEP_ALIVE.getType()
        response.id == null
        response.payload == null
    }
}
