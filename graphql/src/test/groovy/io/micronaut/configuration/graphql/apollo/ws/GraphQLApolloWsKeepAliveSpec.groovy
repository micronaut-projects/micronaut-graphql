package io.micronaut.configuration.graphql.apollo.ws


import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.websocket.WebSocketClient
import reactor.core.publisher.Flux
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Gerard Klijs
 * @since 1.3
 */
class GraphQLApolloWsKeepAliveSpec extends Specification {

    @AutoCleanup
    @Shared
    EmbeddedServer embeddedServer = embeddedServer = ApplicationContext.run(EmbeddedServer, "keepalive") as EmbeddedServer

    @Shared
    WebSocketClient wsClient = embeddedServer.applicationContext.createBean(WebSocketClient, embeddedServer.getURI())

    @Shared
    GraphQLWsClient graphQLWsClient = Flux.from(wsClient.connect(GraphQLWsClient, "/ka-ws")).blockFirst();

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
