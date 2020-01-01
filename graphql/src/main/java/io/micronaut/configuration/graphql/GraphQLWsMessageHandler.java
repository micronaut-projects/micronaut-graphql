package io.micronaut.configuration.graphql;

import graphql.ExecutionResult;
import io.micronaut.core.util.StringUtils;
import io.micronaut.websocket.WebSocketSession;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

import static io.micronaut.configuration.graphql.GraphQLWsResponse.ServerType.GQL_CONNECTION_ACK;
import static io.micronaut.configuration.graphql.GraphQLWsResponse.ServerType.GQL_ERROR;

/**
 * Handles the messages send over the websocket.
 *
 * @author Gerard Klijs
 * @since 1.3
 */
@Singleton
public class GraphQLWsMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQLWsMessageHandler.class);

    private final GraphQLWsState state;
    private final GraphQLInvocation graphQLInvocation;
    private final GraphQLExecutionResultHandler graphQLExecutionResultHandler;
    private final GraphQLWsSender responseSender;

    /**
     * Default constructor.
     *
     * @param state                         the {@link GraphQLWsState} instance
     * @param graphQLInvocation             the {@link GraphQLInvocation} instance
     * @param graphQLExecutionResultHandler the {@link GraphQLExecutionResultHandler} instance
     * @param responseSender                the {@link GraphQLWsSender} instance
     */
    public GraphQLWsMessageHandler(GraphQLWsState state,
            GraphQLInvocation graphQLInvocation,
            GraphQLExecutionResultHandler graphQLExecutionResultHandler,
            GraphQLWsSender responseSender) {
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
     * @return Publisher<String>
     */
    public Publisher<GraphQLWsResponse> handleMessage(GraphQLWsRequest request,
            WebSocketSession session) {
        switch (request.getType()) {
            case GQL_CONNECTION_INIT:
                return Flowable.just(new GraphQLWsResponse(GQL_CONNECTION_ACK));
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

    private Publisher<GraphQLWsResponse> startOperation(GraphQLWsRequest request, WebSocketSession session) {
        if (request.getId() == null) {
            LOG.warn("GraphQL operation id is required with type start");
            return Flowable.just(new GraphQLWsResponse(GQL_ERROR));
        }

        if (state.operationExists(request, session)) {
            LOG.info("Already subscribed to operation {} in session {}", request.getId(), session.getId());
            return Flowable.empty();
        }

        GraphQLRequestBody payload = request.getPayload();
        if (payload == null || StringUtils.isEmpty(payload.getQuery())) {
            LOG.info("Payload was null or query empty for operation {} in session {}", request.getId(),
                     session.getId());
            return Flowable.just(new GraphQLWsResponse(GQL_ERROR, request.getId()));
        }

        return executeRequest(request.getId(), payload, session);
    }

    private Publisher<GraphQLWsResponse> executeRequest(
            String operationId,
            GraphQLRequestBody payload,
            WebSocketSession session) {
        GraphQLInvocationData invocationData = new GraphQLInvocationData(
                payload.getQuery(), payload.getOperationName(), payload.getVariables());
        Publisher<ExecutionResult> executionResult = graphQLInvocation.invoke(invocationData, null);
        Publisher<GraphQLResponseBody> responseBody = graphQLExecutionResultHandler
                .handleExecutionResult(executionResult);
        return Flowable.fromPublisher(responseBody)
                       .flatMap(body -> responseSender.send(operationId, body, session));
    }
}
