/*
 * Copyright 2017-2023 original authors
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

import graphql.ExecutionResult;
import io.micronaut.configuration.graphql.GraphQLConfiguration;
import io.micronaut.configuration.graphql.GraphQLInvocation;
import io.micronaut.configuration.graphql.GraphQLInvocationData;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.codec.CodecException;
import io.micronaut.scheduling.ScheduledExecutorTaskScheduler;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnError;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * WebSocket request handler for the graphql-ws protocol. Supports the 'graphql-transport-ws' WebSocket subprotocol.
 *
 * @author Jeremy Grelle
 * @since 4.0
 */
@ServerWebSocket(
    value = "${" + GraphQLConfiguration.PREFIX + "." + GraphQLWsConfiguration.PATH_CONFIG + ":" + GraphQLWsConfiguration.DEFAULT_PATH + "}",
    subprotocols = "graphql-transport-ws"
)
@Requires(property = GraphQLWsConfiguration.ENABLED_CONFIG, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
public class GraphQLWsHandler {

    static final String HTTP_REQUEST_KEY = "httpRequest";

    private static final Logger LOG = LoggerFactory.getLogger(GraphQLWsHandler.class);

    private final ScheduledExecutorTaskScheduler scheduler;

    private final GraphQLInvocation graphQLInvocation;

    private final GraphQLWsConfiguration configuration;

    private final ConcurrentSkipListSet<String> connections = new ConcurrentSkipListSet<>();

    private final ConcurrentMap<String, Publisher<? extends Message>> subscriptions = new ConcurrentHashMap<>();

    /**
     * Constructor for the graphql-ws WebSocket handler.
     *
     * @param scheduler The task scheduler for handling connection initialisation timeouts.
     * @param graphQLInvocation The graphql invocation helper for executing GraphQL operations.
     * @param configuration The configuration of the graphql-ws support.
     */
    public GraphQLWsHandler(ScheduledExecutorTaskScheduler scheduler, GraphQLInvocation graphQLInvocation, GraphQLWsConfiguration configuration) {
        this.scheduler = scheduler;
        this.graphQLInvocation = graphQLInvocation;
        this.configuration = configuration;
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
        scheduler.schedule(configuration.getConnectionInitWaitTimeout(), () -> {
            if (!connections.contains(session.getId())) {
                session.close(new CloseReason(4408, "Connection initialisation timeout."));
            }
        });
        if (LOG.isTraceEnabled()) {
            LOG.trace("Opened websocket connection with id {}", session.getId());
        }
    }

    /**
     * Called on every message received from the client.
     *
     * @param message Message received from a client
     * @param session WebSocketSession
     * @return {@code Publisher<Message>}
     */
    @OnMessage
    public Publisher<Message> onMessage(
        Message message,
        WebSocketSession session) {
        if (message instanceof ConnectionInitMessage) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received connection initialisation request for session id {}", session.getId());
            }
            return connections.add(session.getId()) ? session.send(new ConnectionAckMessage()) : tooManyInitialisationRequests(session);
        } else if (message instanceof PingMessage) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received a ping message for session id {}", session.getId());
            }
            return session.send(new PongMessage());
        } else if (message instanceof SubscribeMessage m) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received subscription message for session id {}", session.getId());
            }
            if (!connections.contains(session.getId())) {
                return unauthorized(session);
            }
            if (subscriptions.containsKey(m.getId())) {
                return subscriberAlreadyExists(m.getId(), session);
            }
            Publisher<Message> subscription = executeSubscribe(m, session).doFinally(s -> subscriptions.remove(m.getId()));
            subscriptions.put(m.getId(), subscription);
            return subscription;
        } else if (message instanceof CompleteMessage m) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received complete message for session id {}", session.getId());
            }
            subscriptions.remove(m.getId());
        }
        return Mono.empty();
    }

    @SuppressWarnings({"rawtypes"})
    private Mono<Message> executeSubscribe(SubscribeMessage subscribeMessage, WebSocketSession session) {
        GraphQLInvocationData invocationData = new GraphQLInvocationData(subscribeMessage.getSubscribePayload().getQuery(),
            subscribeMessage.getSubscribePayload().getOperationName(), subscribeMessage.getSubscribePayload().getVariables());

        Optional<HttpRequest> httpRequest = session.get(HTTP_REQUEST_KEY, HttpRequest.class);
        if (httpRequest.isEmpty()) {
            return Mono.error(new IllegalStateException("The HTTP request from the original WebSocket connection could not be retrieved."));
        }

        return Flux.from(graphQLInvocation.invoke(invocationData, httpRequest.get(), null))
            .flatMap(executionResult -> {
                if (executionResult.isDataPresent() && executionResult.getData() != null && executionResult.getData() instanceof Publisher<?> p) {
                    return handleExecutionResultPublisher(p);
                }
                return Flux.just(executionResult);
            })
            .takeUntil(e -> !subscriptions.containsKey(subscribeMessage.getId()))
            .flatMap(executionResult -> handleExecutionResult(subscribeMessage, session, executionResult))
            .last()
            .filter(NextMessage.class::isInstance)
            .flatMap(m -> completeSubscription(subscribeMessage, session));
    }

    private Flux<ExecutionResult> handleExecutionResultPublisher(Publisher<?> p) {
        return Flux.from(p).map(o -> {
            if (o instanceof ExecutionResult publishedExecutionResult) {
                return publishedExecutionResult;
            }
            throw new IllegalArgumentException("Subscription data is an invalid type " + o.getClass().getName() + "- expected to be an ExecutionResult");
        });
    }

    private Publisher<Message> handleExecutionResult(SubscribeMessage subscribeMessage, WebSocketSession session, ExecutionResult executionResult) {
        if (!session.isOpen() && subscriptions.containsKey(subscribeMessage.getId())) {
            return Mono.empty();
        }
        if (executionResult.getErrors().isEmpty()) {
            return session.send(new NextMessage(subscribeMessage.getId(), executionResult));
        }
        return session.send(ErrorMessage.of(subscribeMessage.getId(), executionResult.getErrors()));
    }

    private Mono<CompleteMessage> completeSubscription(SubscribeMessage subscribeMessage, WebSocketSession session) {
        return Mono.from(session.isOpen() && subscriptions.containsKey(subscribeMessage.getId())
            ? session.send(new CompleteMessage(subscribeMessage.getId())) : Mono.empty());
    }

    private Publisher<Message> unauthorized(WebSocketSession session) {
        session.close(new CloseReason(4401, "Unauthorized."));
        return Mono.empty();
    }

    private Publisher<Message> tooManyInitialisationRequests(WebSocketSession session) {
        session.close(new CloseReason(4403, "Too many initialisation requests."));
        return Mono.empty();
    }

    private Publisher<Message> subscriberAlreadyExists(String id, WebSocketSession session) {
        session.close(new CloseReason(4409, "Subscriber for " + id + " already exists."));
        return Mono.empty();
    }

    /**
     * Called when the websocket is closed.
     *
     * @param session     The {@link WebSocketSession} being closed.
     * @param closeReason The {@link CloseReason} describing why the socket has been closed.
     */
    @OnClose
    public void onClose(WebSocketSession session, CloseReason closeReason) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Closed websocket connection with id {} with reason {}", session.getId(), closeReason);
        }
    }

    /**
     * Called when there is an error with the websocket. If a JSON decoding error is detected, the socket will be closed
     * with a <code>4400</code> error as defined by the graphql-ws spec.
     *
     * @param session The {@link WebSocketSession} where an error occurred.
     * @param t       {@link Throwable}, the cause of the error
     */
    @OnError
    public void onError(WebSocketSession session, Throwable t) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Error websocket connection with id {} with error {}", session.getId(), t.getMessage());
        }
        if (t instanceof CodecException || t instanceof InstantiationError) {
            session.close(new CloseReason(4400, "Invalid message."));
        }
    }
}
