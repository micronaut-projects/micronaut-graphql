package io.micronaut.configuration.graphql.ws;

import io.micronaut.configuration.graphql.GraphQLConfiguration;
import io.micronaut.configuration.graphql.GraphQLJsonSerializer;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.scheduling.annotation.Scheduled;
import io.micronaut.websocket.WebSocketBroadcaster;

import javax.inject.Singleton;

import static io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType.GQL_CONNECTION_KEEP_ALIVE;

/**
 * Used to send keep alive messages to the active sessions at a regular interval.
 */
@Singleton
@Requires(property = GraphQLWsConfiguration.KEEP_ALIVE_ENABLED,
        value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
public class GraphQLWsKeepAlive {

    private final WebSocketBroadcaster broadcaster;
    private final GraphQLWsState state;
    private final String kaMessage;

    /**
     * Default constructor.
     *
     * @param broadcaster           the {@link WebSocketBroadcaster} instance
     * @param state                 the {@link GraphQLWsState} instance
     * @param graphQLJsonSerializer the {@link GraphQLJsonSerializer} instance
     */
    public GraphQLWsKeepAlive(WebSocketBroadcaster broadcaster, GraphQLWsState state,
            GraphQLJsonSerializer graphQLJsonSerializer) {
        this.broadcaster = broadcaster;
        this.state = state;
        kaMessage = graphQLJsonSerializer.serialize(new GraphQLWsResponse(GQL_CONNECTION_KEEP_ALIVE));
    }

    /**
     * Send ka messages to active sessions.
     */
    @Scheduled(fixedDelay =
            "${" + GraphQLConfiguration.PREFIX + "." + GraphQLWsConfiguration.KEEP_ALIVE_INTERVAL + ":"
                    + GraphQLWsConfiguration.DEFAULT_KEEP_ALIVE_INTERVAL + "}")
    public void keepAliveSender() {
        broadcaster.broadcastSync(kaMessage, state::isActive);
    }
}
