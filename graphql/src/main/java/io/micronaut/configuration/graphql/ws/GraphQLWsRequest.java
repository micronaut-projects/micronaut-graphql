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

import io.micronaut.core.annotation.Nullable;

/**
 * Class to handle the message to and from the websocket.
 *
 * @param <P> The payload type
 * @author Gerard Klijs
 * @since 1.3
 */
public abstract class GraphQLWsRequest<P> {

    private static final String TYPE_ERROR_MESSAGE = "Could not map %s to a known client type.";

    private ClientType type;
    @Nullable
    private String id;
    @Nullable
    private P payload;

    /**
     * Get the type.
     *
     * @return the type of message as ClientType
     */
    public ClientType getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the type as string
     */
    public void setType(final String type) {
        this.type = fromString(type);
    }

    /**
     * Get the id.
     *
     * @return id as string
     */
    @Nullable
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id
     */
    public void setId(@Nullable final String id) {
        this.id = id;
    }

    /**
     * Get the payload.
     *
     * @return the payload
     */
    @Nullable
    public P getPayload() {
        return payload;
    }

    /**
     * Sets the payload.
     *
     * @param payload the payload
     */
    public void setPayload(@Nullable final P payload) {
        this.payload = payload;
    }

    static ClientType fromString(String type) {
        for (ClientType clientType : ClientType.values()) {
            if (clientType.getType().equals(type)) {
                return clientType;
            }
        }
        throw new RuntimeException(String.format(TYPE_ERROR_MESSAGE, type));
    }

    /**
     * Types of messages received from the client.
     */
    public enum ClientType {
        GQL_CONNECTION_INIT("connection_init"),
        GQL_START("start"),
        GQL_STOP("stop"),
        GQL_CONNECTION_TERMINATE("connection_terminate");

        private final String type;

        /**
         * Default constructor.
         *
         * @param type string
         */
        ClientType(String type) {
            this.type = type;
        }

        /**
         * Get the type.
         *
         * @return type as string
         */
        public String getType() {
            return type;
        }
    }
}
