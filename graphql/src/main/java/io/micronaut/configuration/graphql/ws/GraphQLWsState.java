/*
 * Copyright 2017-2020 original authors
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

import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

import static io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType.GQL_COMPLETE;

/**
 * Keeps the state of the web socket subscriptions.
 *
 * @author Gerard Klijs
 * @since 1.3
 */
@Singleton
class GraphQLWsState {

    private ConcurrentSkipListSet<String> activeSessions = new ConcurrentSkipListSet<>();
    private ConcurrentHashMap<String, GraphQLWsOperations> activeOperations = new ConcurrentHashMap<>();

    /**
     * Sets the session to active.
     *
     * @param session WebSocketSession
     */
    void activateSession(WebSocketSession session) {
        activeSessions.add(session.getId());
    }

    /**
     * Whether the session is considered active, which means the client called init but not yet terminate.
     *
     * @param session WebSocketSession
     * @return whether the session is active
     */
    boolean isActive(WebSocketSession session) {
        return activeSessions.contains(session.getId());
    }

    /**
     * Sets the GraphQLWsOperations for the client.
     *
     * @param session WebSocketSession
     */
    void init(WebSocketSession session) {
        activeOperations.putIfAbsent(session.getId(), new GraphQLWsOperations());
    }

    /**
     * Stop and remove all subscriptions for the session.
     *
     * @param session WebSocketSession
     * @return Publisher<GraphQLWsOperationMessage>
     */
    Publisher<GraphQLWsResponse> terminateSession(WebSocketSession session) {
        activeSessions.remove(session.getId());
        Optional.ofNullable(activeOperations.remove(session.getId()))
                .ifPresent(GraphQLWsOperations::cancelAll);
        return Flux.empty();
    }

    /**
     * Saves the operation under the client.id and operation.id so it can be cancelled later.
     *
     * @param operationId String
     * @param session     WebSocketSession
     * @param starter     Function to start the subscription, will only be called if not already present
     */
    void saveOperation(String operationId, WebSocketSession session, Function<String, Subscription> starter) {
        Optional.ofNullable(session)
                .map(WebSocketSession::getId)
                .map(id -> activeOperations.get(id))
                .ifPresent(graphQLWsOperations -> graphQLWsOperations.addSubscription(operationId, starter));
    }

    /**
     * Stops the current operation is present and returns the proper message.
     *
     * @param request GraphQLWsRequest
     * @param session WebSocketSession
     * @return the complete message, or nothing if there was no operation running
     */
    Publisher<GraphQLWsResponse> stopOperation(GraphQLWsRequest request, WebSocketSession session) {
        String sessionId = session.getId();
        String operationId = request.getId();
        if (operationId == null || sessionId == null) {
            return Flux.empty();
        }
        boolean removed = Optional.ofNullable(activeOperations.get(sessionId))
                                  .map(graphQLWsOperations -> {
                                      graphQLWsOperations.cancelOperation(operationId);
                                      return graphQLWsOperations.removeCompleted(operationId);
                                  }).orElse(false);
        return removed ? Flux.just(new GraphQLWsResponse(GQL_COMPLETE, operationId)) : Flux.empty();
    }

    /**
     * Remove the operation once completed, to clean up and prevent sending a second complete on stop.
     *
     * @param operationId String
     * @param session     WebSocketSession
     * @return whether the operation was removed
     */
    boolean removeCompleted(String operationId, WebSocketSession session) {
        return Optional.ofNullable(session)
                       .map(WebSocketSession::getId)
                       .map(sessionId -> activeOperations.get(sessionId))
                       .map(graphQLWsOperations -> graphQLWsOperations.removeCompleted(operationId))
                       .orElse(false);
    }

    /**
     * Returns whether the operation already exists.
     *
     * @param request GraphQLWsRequest
     * @param session WebSocketSession
     * @return true or false
     */
    boolean operationExists(GraphQLWsRequest request, WebSocketSession session) {
        return Optional.ofNullable(session)
                       .map(WebSocketSession::getId)
                       .map(sessionId -> activeOperations.get(sessionId))
                       .map(graphQLWsOperations -> graphQLWsOperations.operationExists(request))
                       .orElse(false);
    }
}
