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
        GraphQLApolloWsRequest request = new GraphQLApolloWsRequest()
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_CONNECTION_INIT.getType())

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLApolloWsResponse response = graphQLWsClient.nextResponse()
        response.getType() == GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_ACK.getType()
        GraphQLApolloWsResponse firstAk = graphQLWsClient.nextResponse()
        firstAk != null
        GraphQLApolloWsResponse secondAk = graphQLWsClient.nextResponse()
        secondAk != null
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_CONNECTION_TERMINATE.getType())
        graphQLWsClient.send(request)
        GraphQLApolloWsResponse noResponse = graphQLWsClient.nextResponse()
        noResponse == null

        and:
        firstAk.type == GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_KEEP_ALIVE.getType()
        secondAk.type == GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_KEEP_ALIVE.getType()
        response.id == null
        response.payload == null
    }
}
