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
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnError;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType.GQL_CONNECTION_ERROR;

/**
 * The GraphQL websocket controller handling GraphQL requests.
 * Implementation of https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md
 *
 * @author Gerard Klijs
 * @since 1.3
 */
@ServerWebSocket(value =
        "${" + GraphQLConfiguration.PREFIX + "." + GraphQLWsConfiguration.PATH + ":"
                + GraphQLWsConfiguration.DEFAULT_PATH + "}", subprotocols = "graphql-ws")
@Requires(property = GraphQLWsConfiguration.ENABLED, value = StringUtils.TRUE, defaultValue =
        StringUtils.FALSE)
public class GraphQLWsController {

    static final String HTTP_REQUEST_KEY = "httpRequest";
    private static final Logger LOG = LoggerFactory.getLogger(GraphQLWsController.class);

    private final GraphQLWsMessageHandler messageHandler;
    private final GraphQLWsState state;
    private final GraphQLJsonSerializer graphQLJsonSerializer;
    private final GraphQLWsResponse errorMessage;

    /**
     * Default constructor.
     *
     * @param messageHandler        the {@link GraphQLWsMessageHandler} instance
     * @param state                 the {@link GraphQLWsState} instance
     * @param graphQLJsonSerializer the {@link GraphQLJsonSerializer} instance
     */
    public GraphQLWsController(GraphQLWsMessageHandler messageHandler, GraphQLWsState state,
            GraphQLJsonSerializer graphQLJsonSerializer) {
        this.messageHandler = messageHandler;
        this.state = state;
        this.graphQLJsonSerializer = graphQLJsonSerializer;
        errorMessage = new GraphQLWsResponse(GQL_CONNECTION_ERROR);
    }

    /**
     * Called when the connection is opened. We store the original request, since it might be needed for the
     * GraphQLInvocation.
     *
     * @param session WebSocketSession
     * @param request HttpRequest
     */
    @OnOpen
    @SuppressWarnings("rawtypes")
    public void onOpen(WebSocketSession session, HttpRequest request) {
        session.put(HTTP_REQUEST_KEY, request);
        state.init(session);
        LOG.info("Opened websocket connection with id {}", session.getId());
    }


    /**
     * Called on every message received from the client.
     *
     * @param message Message received from a client
     * @param session WebSocketSession
     * @return Publisher<GraphQLWsResponse>
     */
    @OnMessage
    public Publisher<GraphQLWsResponse> onMessage(
            String message,
            WebSocketSession session) {
        try {
            GraphQLWsRequest request = graphQLJsonSerializer.deserialize(message, GraphQLWsRequest.class);
            if (request.getType() == null) {
                LOG.warn("Type was null on operation message");
                return send(Flowable.just(errorMessage), session);
            } else {
                return send(messageHandler.handleMessage(request, session), session);
            }
        } catch (Exception e) {
            LOG.warn("Error deserializing message received from client: {}", message, e);
            return send(Flowable.just(errorMessage), session);
        }
    }

    /**
     * Called when the websocket is closed.
     *
     * @param session     WebSocketSession
     * @param closeReason CloseReason
     * @return Publisher<GraphQLWsResponse>
     */
    @OnClose
    public Publisher<GraphQLWsResponse> onClose(WebSocketSession session, CloseReason closeReason) {
        LOG.info("Closed websocket connection with id {} with reason {}", session.getId(), closeReason);
        return send(state.terminateSession(session), session);
    }

    /**
     * Called when there is an error with the websocket, this probably means the connection is lost, but hasn't been
     * properly closed.
     *
     * @param session WebSocketSession
     * @param t       Throwable, the cause of the error
     * @return Publisher<GraphQLWsResponse>
     */
    @OnError
    public Publisher<GraphQLWsResponse> onError(WebSocketSession session, Throwable t) {
        LOG.info("Error websocket connection with id {} with error {}", session.getId(), t.getMessage());
        return send(state.terminateSession(session), session);
    }

    private Publisher<GraphQLWsResponse> send(Publisher<GraphQLWsResponse> publisher, WebSocketSession session) {
        return Publishers.then(publisher, response -> {
            if (session.isOpen()) {
                session.sendSync(graphQLJsonSerializer.serialize(response));
            }
        });
    }
}
