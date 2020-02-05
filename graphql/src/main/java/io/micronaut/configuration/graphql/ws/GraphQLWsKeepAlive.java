/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 *
 * @author Gerard Klijs
 * @since 1.3
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
