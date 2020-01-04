package io.micronaut.configuration.graphql;

import io.micronaut.websocket.WebSocketSession;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

import static io.micronaut.configuration.graphql.GraphQLWsResponse.ServerType.GQL_COMPLETE;

/**
 * Keeps the state of the web socket subscriptions.
 *
 * @author Gerard Klijs
 * @since 1.3
 */
@Singleton
class GraphQLWsState {

    private ConcurrentSkipListSet<String> activeSessions = new ConcurrentSkipListSet<>();
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Subscription>> activeOperations =
            new ConcurrentHashMap<>();

    /**
     * Sests the session to active.
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
     * Stop and remove all subscriptions for the session.
     *
     * @param session WebSocketSession
     * @return Publisher<GraphQLWsOperationMessage>
     */
    Publisher<GraphQLWsResponse> terminateSession(WebSocketSession session) {
        activeSessions.remove(session.getId());
        if (activeOperations.containsKey(session.getId())) {
            for (Subscription subscription : activeOperations.get(session.getId()).values()) {
                subscription.cancel();
            }
            activeOperations.remove(session.getId());
        }
        return Flowable.empty();
    }

    /**
     * Saves the operation under the client.id and operation.id so it can be cancelled later.
     *
     * @param operationId String
     * @param session     WebSocketSession
     * @param starter     Function to start the subscription, will only be called if not already present
     */
    void saveOperation(String operationId, WebSocketSession session, Function<String, Subscription> starter) {
        activeOperations.putIfAbsent(session.getId(), new ConcurrentHashMap<>());
        activeOperations.get(session.getId()).computeIfAbsent(operationId, starter);
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
        if (operationId != null && activeOperations.containsKey(sessionId) &&
                activeOperations.get(sessionId).containsKey(operationId)) {
            activeOperations.get(sessionId).get(operationId).cancel();
            activeOperations.get(sessionId).remove(operationId);
            if (activeOperations.get(sessionId).isEmpty()) {
                activeOperations.remove(sessionId);
            }
            return Flowable.just(new GraphQLWsResponse(GQL_COMPLETE, operationId));
        }
        return Flowable.empty();
    }

    /**
     * Remove the operation once completed, to clean up and prevent sending a second complete on stop.
     *
     * @param operationId String
     * @param session     WebSocketSession
     */
    void removeCompleted(String operationId, WebSocketSession session) {
        String sessionId = session.getId();
        if (operationId != null && activeOperations.containsKey(sessionId) &&
                activeOperations.get(sessionId).containsKey(operationId)) {
            activeOperations.get(sessionId).remove(operationId);
            if (activeOperations.get(sessionId).isEmpty()) {
                activeOperations.remove(sessionId);
            }
        }
    }

    /**
     * Returns whether the operation already exists.
     *
     * @param request GraphQLWsRequest
     * @param session WebSocketSession
     * @return true or false
     */
    boolean operationExists(GraphQLWsRequest request, WebSocketSession session) {
        ConcurrentHashMap<String, Subscription> operations = activeOperations.get(session.getId());
        return operations != null && operations.containsKey(request.getId());
    }
}
