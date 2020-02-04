package io.micronaut.configuration.graphql.ws;

import org.reactivestreams.Subscription;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Keeps the state of the web socket subscriptions for one connection.
 *
 * @author Gerard Klijs
 * @since 1.3
 */
@Singleton
class GraphQLWsOperations {

    private ConcurrentHashMap<String, Subscription> activeOperations = new ConcurrentHashMap<>();

    /**
     * Cancels all containing subscriptions.
     */
    void cancelAll() {
        for (Subscription subscription : activeOperations.values()) {
            subscription.cancel();
        }
    }

    /**
     * Add the subscription to the map, done with a function such that when it already exists a second subscription
     * is not started.
     *
     * @param operationId String
     * @param starter     function to start a subscription,
     */
    void addSubscription(String operationId, Function<String, Subscription> starter) {
        activeOperations.computeIfAbsent(operationId, starter);
    }

    /**
     * Cancels the operation if it exists.
     *
     * @param operationId String
     */
    void cancelOperation(String operationId) {
        Optional.ofNullable(activeOperations.get(operationId)).ifPresent(Subscription::cancel);
    }

    /**
     * Remove the operation once completed, to clean up and prevent sending a second complete on stop.
     *
     * @param operationId String
     * @return whether the operation was removed
     */
    boolean removeCompleted(String operationId) {
        if (operationId != null) {
            return activeOperations.remove(operationId) != null;
        } else {
            return false;
        }
    }

    /**
     * Whether the operation currently already exists.
     *
     * @param request the {@link GraphQLWsRequest} instance
     * @return whether it exists or not
     */
    boolean operationExists(GraphQLWsRequest request) {
        return Optional.ofNullable(request)
                       .map(GraphQLWsRequest::getId)
                       .map(id -> activeOperations.containsKey(id))
                       .orElse(false);
    }
}
