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
package io.micronaut.configuration.graphql.apollo.ws;

import graphql.ExecutionResult;
import io.micronaut.configuration.graphql.GraphQLJsonSerializer;
import io.micronaut.configuration.graphql.GraphQLResponseBody;
import io.micronaut.core.async.subscriber.CompletionAwareSubscriber;
import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.function.Function;

import static io.micronaut.configuration.graphql.apollo.ws.GraphQLApolloWsResponse.ServerType.GQL_COMPLETE;
import static io.micronaut.configuration.graphql.apollo.ws.GraphQLApolloWsResponse.ServerType.GQL_DATA;
import static io.micronaut.configuration.graphql.apollo.ws.GraphQLApolloWsResponse.ServerType.GQL_ERROR;

/**
 * Sends the GraphQL response(s) to the client.
 *
 * @author Gerard Klijs
 * @since 1.3
 */
@Singleton
public class GraphQLApolloWsSender {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQLApolloWsSender.class);

    private final GraphQLApolloWsState state;
    private final GraphQLJsonSerializer graphQLJsonSerializer;

    /**
     * Default constructor.
     *
     * @param state                 the {@link GraphQLApolloWsState} instance
     * @param graphQLJsonSerializer the {@link GraphQLJsonSerializer} instance
     */
    public GraphQLApolloWsSender(GraphQLApolloWsState state, GraphQLJsonSerializer graphQLJsonSerializer) {
        this.state = state;
        this.graphQLJsonSerializer = graphQLJsonSerializer;
    }

    /**
     * Transform the result from the a websocket request to a message that can be send to the client.
     *
     * @param operationId  String value of the operation id
     * @param responseBody GraphQLResponseBody of the executed operation
     * @param session      The websocket session by which the operation was executed
     * @return GraphQLWsOperationMessage
     */
    @SuppressWarnings("unchecked")
    Publisher<GraphQLApolloWsResponse> send(String operationId, GraphQLResponseBody responseBody, WebSocketSession session) {
        Object dataObject = responseBody.getSpecification().get("data");
        if (dataObject instanceof Publisher) {
            startSubscription(operationId, (Publisher<ExecutionResult>) dataObject, session);
            return Flux.empty();
        }
        return Flux.just(toGraphQLApolloWsResponse(operationId, responseBody), new GraphQLApolloWsResponse(GQL_COMPLETE, operationId));
    }

    private GraphQLApolloWsResponse toGraphQLApolloWsResponse(String operationId, GraphQLResponseBody responseBody) {
        if (hasErrors(responseBody)) {
            return new GraphQLApolloWsResponse(GQL_ERROR, operationId, responseBody);
        } else {
            return new GraphQLApolloWsResponse(GQL_DATA, operationId, responseBody);
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean hasErrors(GraphQLResponseBody responseBody) {
        if (responseBody.getSpecification().get("errors") instanceof Collection errorObject) {
            return !errorObject.isEmpty();
        } else {
            return false;
        }
    }

    private Function<String, Subscription> starter(Publisher<ExecutionResult> publisher, WebSocketSession session) {
        return operationId -> {
            SendSubscriber subscriber = new SendSubscriber(operationId, session);
            publisher.subscribe(subscriber);
            return subscriber.getSubscription();
        };
    }

    private void startSubscription(String operationId, Publisher<ExecutionResult> publisher,
            WebSocketSession session) {
        state.saveOperation(operationId, session, starter(publisher, session));
    }

    /**
     * Subscriber to handle the messages, might be cancelled when the client calls stop or when the connection is
     * broken.
     */
    private final class SendSubscriber extends CompletionAwareSubscriber<ExecutionResult> {

        private final String operationId;
        private final WebSocketSession session;

        private SendSubscriber(String operationId, WebSocketSession session) {
            this.operationId = operationId;
            this.session = session;
        }

        Subscription getSubscription() {
            return subscription;
        }

        @Override
        protected void doOnSubscribe(Subscription subscription) {
            LOG.info("Subscribed to results for to operation {} in session {}", operationId, session.getId());
            subscription.request(1L);
        }

        @Override
        protected void doOnNext(ExecutionResult message) {
            convertAndSend(message);
            subscription.request(1L);
        }

        @Override
        protected void doOnError(Throwable t) {
            LOG.warn("Error in SendSubscriber", t);
            send(new GraphQLApolloWsResponse(GQL_ERROR, operationId));
        }

        @Override
        protected void doOnComplete() {
            LOG.info("Completed results for operation {} in session {}", operationId, session.getId());
            if (state.removeCompleted(operationId, session)) {
                send(new GraphQLApolloWsResponse(GQL_COMPLETE, operationId));
            }
        }

        private void convertAndSend(ExecutionResult executionResult) {
            GraphQLApolloWsResponse response = toGraphQLApolloWsResponse(
                    operationId, new GraphQLResponseBody(executionResult.toSpecification()));
            send(response);
        }

        private void send(GraphQLApolloWsResponse response) {
            if (session.isOpen()) {
                session.sendSync(graphQLJsonSerializer.serialize(response));
            }
        }
    }
}
