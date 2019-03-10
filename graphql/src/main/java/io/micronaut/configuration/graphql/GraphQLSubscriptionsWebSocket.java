/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.configuration.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnError;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import io.reactivex.Single;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;

/**
 * The GraphQL subscriptions web socket handling GraphQL subscriptions.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
@ServerWebSocket("${" + GraphQLConfiguration.PREFIX + "." + GraphQLConfiguration.GraphQLSubscriptionsConfiguration.PATH + ":"
        + GraphQLConfiguration.GraphQLSubscriptionsConfiguration.DEFAULT_PATH + "}")
@Requires(property = GraphQLConfiguration.GraphQLSubscriptionsConfiguration.ENABLED,
        value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
public class GraphQLSubscriptionsWebSocket {

    /**
     * TODO.
     *
     * @param session the web socket session
     */
    @OnOpen
    public void onOpen(WebSocketSession session) {
        System.out.println("onOpen...");
        // TODO.
    }

    /**
     * TODO.
     *
     * @param body    the message body
     * @param session the web socket session
     */
    @OnMessage
    public void onMessage(String body, WebSocketSession session) throws IOException {
        System.out.println("onMessage...");
        System.out.println("body:\n" + body);
        // TODO.
    }

    /**
     * TODO.
     *
     * @param session the web socket session
     */
    @OnError
    public void onError(WebSocketSession session) {
        System.out.println("onError...");
        // TODO.
    }

    /**
     * TODO.
     *
     * @param session the web socket session
     */
    @OnClose
    public void onClose(WebSocketSession session) {
        System.out.println("onClose...");
        // TODO.
    }
}
