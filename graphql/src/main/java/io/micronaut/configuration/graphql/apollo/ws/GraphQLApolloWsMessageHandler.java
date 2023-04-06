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
import io.micronaut.configuration.graphql.GraphQLExecutionResultHandler;
import io.micronaut.configuration.graphql.GraphQLInvocation;
import io.micronaut.configuration.graphql.GraphQLInvocationData;
import io.micronaut.configuration.graphql.GraphQLRequestBody;
import io.micronaut.configuration.graphql.GraphQLResponseBody;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import static io.micronaut.configuration.graphql.apollo.ws.GraphQLWsController.HTTP_REQUEST_KEY;
import static io.micronaut.configuration.graphql.apollo.ws.GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_ACK;
import static io.micronaut.configuration.graphql.apollo.ws.GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_KEEP_ALIVE;
import static io.micronaut.configuration.graphql.apollo.ws.GraphQLApolloWsResponse.ServerType.GQL_ERROR;

/**
 * Handles the messages send over the websocket.
 *
 * @author Gerard Klijs
 * @since 1.3
 */
@Singleton
public class GraphQLApolloWsMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQLApolloWsMessageHandler.class);

    private final GraphQLApolloWsConfiguration graphQLApolloWsConfiguration;
    private final GraphQLApolloWsState state;
    private final GraphQLInvocation graphQLInvocation;
    private final GraphQLExecutionResultHandler graphQLExecutionResultHandler;
    private final GraphQLApolloWsSender responseSender;

    /**
     * Default constructor.
     *
     * @param graphQLApolloWsConfiguration        the {@link GraphQLApolloWsConfiguration} instance
     * @param state                         the {@link GraphQLApolloWsState} instance
     * @param graphQLInvocation             the {@link GraphQLInvocation} instance
     * @param graphQLExecutionResultHandler the {@link GraphQLExecutionResultHandler} instance
     * @param responseSender                the {@link GraphQLApolloWsSender} instance
     */
    public GraphQLApolloWsMessageHandler(
            GraphQLApolloWsConfiguration graphQLApolloWsConfiguration,
            GraphQLApolloWsState state,
            GraphQLInvocation graphQLInvocation,
            GraphQLExecutionResultHandler graphQLExecutionResultHandler,
            GraphQLApolloWsSender responseSender) {
        this.graphQLApolloWsConfiguration = graphQLApolloWsConfiguration;
        this.state = state;
        this.graphQLInvocation = graphQLInvocation;
        this.graphQLExecutionResultHandler = graphQLExecutionResultHandler;
        this.responseSender = responseSender;
    }

    /**
     * Handles the request possibly invocating graphql.
     *
     * @param request Message from client
     * @param session WebSocketSession
     * @return Publisher<GraphQLWsResponse>
     */
    public Publisher<GraphQLApolloWsResponse> handleMessage(GraphQLApolloWsRequest request, WebSocketSession session) {
        switch (request.getType()) {
            case GQL_CONNECTION_INIT:
                return init(session);
            case GQL_START:
                return startOperation(request, session);
            case GQL_STOP:
                return state.stopOperation(request, session);
            case GQL_CONNECTION_TERMINATE:
                return state.terminateSession(session);
            default:
                throw new IllegalStateException("Unexpected value: " + request.getType());
        }
    }

    private Publisher<GraphQLApolloWsResponse> init(WebSocketSession session) {
        if (graphQLApolloWsConfiguration.keepAliveEnabled) {
            state.activateSession(session);
            return Flux.just(new GraphQLApolloWsResponse(GQL_CONNECTION_ACK),
                    new GraphQLApolloWsResponse(GQL_CONNECTION_KEEP_ALIVE));
        } else {
            return Flux.just(new GraphQLApolloWsResponse(GQL_CONNECTION_ACK));
        }
    }

    private Publisher<GraphQLApolloWsResponse> startOperation(GraphQLApolloWsRequest request, WebSocketSession session) {
        if (request.getId() == null) {
            LOG.warn("GraphQL operation id is required with type start");
            return Flux.just(new GraphQLApolloWsResponse(GQL_ERROR));
        }

        if (state.operationExists(request, session)) {
            LOG.info("Already subscribed to operation {} in session {}", request.getId(), session.getId());
            return Flux.empty();
        }

        GraphQLRequestBody payload = request.getPayload();
        if (payload == null || StringUtils.isEmpty(payload.getQuery())) {
            LOG.info("Payload was null or query empty for operation {} in session {}", request.getId(),
                    session.getId());
            return Flux.just(new GraphQLApolloWsResponse(GQL_ERROR, request.getId()));
        }

        return executeRequest(request.getId(), payload, session);
    }

    @SuppressWarnings("rawtypes")
    private Publisher<GraphQLApolloWsResponse> executeRequest(
            String operationId,
            GraphQLRequestBody payload,
            WebSocketSession session) {
        GraphQLInvocationData invocationData = new GraphQLInvocationData(
                payload.getQuery(), payload.getOperationName(), payload.getVariables());
        HttpRequest httpRequest = session
                .get(HTTP_REQUEST_KEY, HttpRequest.class)
                .orElseThrow(() -> new RuntimeException("HttpRequest could not be retrieved from websocket session"));
        Publisher<ExecutionResult> executionResult = graphQLInvocation.invoke(invocationData, httpRequest, null);
        Publisher<GraphQLResponseBody> responseBody = graphQLExecutionResultHandler.handleExecutionResult(executionResult);
        return Flux.from(responseBody).flatMap(body -> responseSender.send(operationId, body, session));
    }
}
