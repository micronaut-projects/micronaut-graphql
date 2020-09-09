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
package io.micronaut.configuration.graphql;

import graphql.GraphQLContext;
import io.micronaut.websocket.WebSocketSession;

/**
 * Constant definitions for the standard keys used by this plugin in {@link GraphQLContext} or {@link WebSocketSession}.
 *
 * @since 2.1.0
 */
public final class GraphQLContextKeys {

    private GraphQLContextKeys() {
    }

    /**
     * Key for storing current {@link io.micronaut.http.HttpRequest} instance.
     */
    public static final String HTTP_REQUEST_KEY = "httpRequest";

    /**
     * Key for storing current {@link io.micronaut.http.HttpResponse} instance.
     */
    public static final String HTTP_RESPONSE_KEY = "httpResponse";

}
