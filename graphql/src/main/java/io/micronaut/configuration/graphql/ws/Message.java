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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


/**
 * A class for mapping graphql-ws messages.
 *
 * @author Jeremy Grelle
 * @since 4.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @Type(value = ConnectionInitMessage.class, name = Message.Types.CONNECTION_INIT),
    @Type(value = ConnectionAckMessage.class, name = Message.Types.CONNECTION_ACK),
    @Type(value = PingMessage.class, name = Message.Types.PING),
    @Type(value = PongMessage.class, name = Message.Types.PONG),
    @Type(value = SubscribeMessage.class, name = Message.Types.SUBSCRIBE),
    @Type(value = NextMessage.class, name = Message.Types.NEXT),
    @Type(value = ErrorMessage.class, name = Message.Types.ERROR),
    @Type(value = CompleteMessage.class, name = Message.Types.COMPLETE)
})
public abstract sealed class Message {

    /**
     * Get the required value of the message's <code>type</code> field.
     *
     * @return The message's type
     */
    @JsonIgnore
    @NonNull
    abstract String getMessageType();

    /**
     * The allowable graphql-ws message types.
     */
    public static class Types {
        public static final String CONNECTION_INIT = "connection_init";
        public static final String CONNECTION_ACK = "connection_ack";
        public static final String PING = "ping";
        public static final String PONG = "pong";
        public static final String SUBSCRIBE = "subscribe";
        public static final String NEXT = "next";
        public static final String ERROR = "error";
        public static final String COMPLETE = "complete";
    }
}

/**
 * A graphql-ws message that contains an optional payload.
 *
 * @param <T> The payload type
 */
abstract sealed class PayloadMessage<T> extends Message {

    @Nullable
    private final T payload;

    /**
     * Default constructor for a graphql-ws message with an optional payload.
     */
    public PayloadMessage() {
        this(null);
    }

    /**
     * Constructor for a graphql-ws message with a payload.
     *
     * @param payload The message payload.
     */
    public PayloadMessage(@Nullable T payload) {
        this.payload = payload;
    }

    /**
     * Get the message payload.
     *
     * @return The message payload.
     */
    @Nullable
    public T getPayload() {
        return payload;
    }
}

/**
 * A graphql-ws message that has a required non-null payload.
 *
 * @param <T> The payload type.
 */
abstract sealed class RequiredPayloadMessage<T> extends Message {

    @NonNull
    private final T payload;

    /**
     * Constructor for a graphql-ws message with a required payload.
     *
     * @param payload The message payload.
     */
    public RequiredPayloadMessage(@NonNull T payload) {
        Objects.requireNonNull(payload, "A payload is required for message type '" + getMessageType() + ".");
        this.payload = payload;
    }

    /**
     * Get the message payload - will never be <code>null</code>.
     *
     * @return The message payload.
     */
    @NonNull
    public T getPayload() {
        return payload;
    }
}

/**
 * A graphql-ws message for connection initialisation.
 */
final class ConnectionInitMessage extends PayloadMessage<Map<String, Object>> {

    /**
     * Get the required value of the message's <code>type</code> field.
     *
     * @return The message's type
     */
    @Override
    @JsonIgnore
    @NonNull
    String getMessageType() {
        return Types.CONNECTION_INIT;
    }
}

/**
 * A graphql-ws message for connection acknowledgement.
 */
final class ConnectionAckMessage extends PayloadMessage<Map<String, Object>> {

    /**
     * Get the required value of the message's <code>type</code> field.
     *
     * @return The message's type
     */
    @Override
    @JsonIgnore
    @NonNull
    String getMessageType() {
        return Types.CONNECTION_ACK;
    }
}

/**
 * A graphql-ws message for a ping.
 */
final class PingMessage extends PayloadMessage<Map<String, Object>> {

    /**
     * Get the required value of the message's <code>type</code> field.
     *
     * @return The message's type
     */
    @Override
    @JsonIgnore
    @NonNull
    String getMessageType() {
        return Types.PING;
    }
}

/**
 * A graphql-ws message for a pong.
 */
final class PongMessage extends PayloadMessage<Map<String, Object>> {

    /**
     * Get the required value of the message's <code>type</code> field.
     *
     * @return The message's type
     */
    @Override
    @JsonIgnore
    @NonNull
    String getMessageType() {
        return Types.PONG;
    }
}

/**
 * A graphql-ws message for encoding the 'next' result from an executed operation.
 */
final class NextMessage extends RequiredPayloadMessage<Map<String, Object>> {

    @NonNull
    private final String id;

    /**
     * Constructor for a graphql-ws 'next' message.
     *
     * @param id      The required non-empty id of the message.
     * @param payload The required non-null payload of the message.
     */
    @JsonCreator
    public NextMessage(@NonNull @JsonProperty("id") String id, @NonNull @JsonProperty("payload") Map<String, Object> payload) {
        super(payload);
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("'id' is required for messages with type '" + getMessageType() + "'.");
        }
        this.id = id;
    }

    /**
     * Constructor for a graphql-ws 'next' message.
     *
     * @param id      The required non-empty id of the message.
     * @param payload The required non-null payload of the message.
     */
    public NextMessage(@NonNull String id, @NonNull ExecutionResult payload) {
        this(id, payload.toSpecification());
    }

    /**
     * Get the required non-empty message id.
     *
     * @return The message id.
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Get the required value of the message's <code>type</code> field.
     *
     * @return The message's type
     */
    @Override
    @JsonIgnore
    @NonNull
    String getMessageType() {
        return Types.NEXT;
    }
}

/**
 * A graphql-ws message for subscribing to the execution of a query.
 */
final class SubscribeMessage extends RequiredPayloadMessage<Map<String, Object>> {
    //TODO - this should be a RequiredPayloadMessage<SubscribeMessage.SubscribePayload>, but Micronaut fails on JSON serialization with a BeanIntrospection error
    @NonNull
    private final String id;

    /**
     * Constructor for a graphql-ws 'subscribe' message.
     *
     * @param id
     * @param payload
     */
    @JsonCreator
    public SubscribeMessage(@NonNull @JsonProperty("id") String id, @NonNull @JsonProperty("payload") SubscribePayload payload) {
        super(payload.toMap());
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("'id' is required for messages with type '" + getMessageType() + "'.");
        }
        this.id = id;
    }

    /**
     * Get the required non-empty message id.
     *
     * @return The message id.
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Get the required value of the message's <code>type</code> field.
     *
     * @return The message's type
     */
    @Override
    @JsonIgnore
    @NonNull
    String getMessageType() {
        return Types.SUBSCRIBE;
    }

    /**
     * Get the message payload as a {@link SubscribePayload}.
     *
     * @return The message payload.
     */
    @JsonIgnore
    @NonNull
    SubscribePayload getSubscribePayload() {
        return SubscribePayload.fromMap(getPayload());
    }

    public static class SubscribePayload {

        @NonNull
        private final String query;

        @Nullable
        private String operationName;

        @Nullable
        private Map<String, Object> variables;

        @Nullable
        private Map<String, Object> extensions;

        /**
         * Constructor for a graphql-ws 'subscribe' message's payload.
         *
         * @param query The required non-empty query being executed and subscribed.
         */
        @JsonCreator
        public SubscribePayload(@NonNull @JsonProperty("query") String query) {
            if (StringUtils.isEmpty(query)) {
                throw new IllegalArgumentException("The 'query' field is required in the payload of message type '" + Types.SUBSCRIBE + "'");
            }
            this.query = query;
        }

        private SubscribePayload(@NonNull String query, @Nullable String operationName, @Nullable Map<String, Object> variables, @Nullable Map<String, Object> extensions) {
            this.query = query;
            this.operationName = operationName;
            this.variables = variables;
            this.extensions = extensions;
        }

        /**
         * Gets the required non-empty query field of the message payload.
         *
         * @return The query.
         */
        @NonNull
        public String getQuery() {
            return query;
        }

        /**
         * Gets the operation name of the payload.
         *
         * @return The operation name.
         */
        @Nullable
        public String getOperationName() {
            return operationName;
        }

        /**
         * Sets the operation name of the payload.
         *
         * @param operationName The operation name.
         */
        public void setOperationName(@Nullable String operationName) {
            this.operationName = operationName;
        }

        /**
         * Gets the variables of the payload.
         *
         * @return The payload variables.
         */
        @Nullable
        public Map<String, Object> getVariables() {
            return variables;
        }

        /**
         * Sets the variables of the payload.
         *
         * @param variables The operation name.
         */
        public void setVariables(@Nullable Map<String, Object> variables) {
            this.variables = variables;
        }

        /**
         * Gets the extensions of the payload.
         *
         * @return The extensions.
         */
        @Nullable
        public Map<String, Object> getExtensions() {
            return extensions;
        }

        /**
         * Sets the extensions of the payload.
         *
         * @param extensions The operation name.
         */
        public void setExtensions(@Nullable Map<String, Object> extensions) {
            this.extensions = extensions;
        }

        public Map<String, Object> toMap() {
            return Map.of(
                "query", this.query,
                "operationName", Optional.ofNullable(this.operationName).orElse(""),
                "variables", Optional.ofNullable(this.variables).orElse(new HashMap<>()),
                "extensions", Optional.ofNullable(this.extensions).orElse(new HashMap<>())
            );
        }

        @SuppressWarnings("unchecked")
        public static SubscribePayload fromMap(Map<String, Object> payload) {
            return new SubscribePayload(
                (String) payload.get("query"),
                (String) payload.get("operationName"),
                (Map<String, Object>) payload.get("variables"),
                (Map<String, Object>) payload.get("extensions"));
        }
    }
}

/**
 * A graphql-ws message for reporting errors pertaining to a specific subscription.
 */
final class ErrorMessage extends RequiredPayloadMessage<List<Map<String, Object>>> {

    @NonNull
    private final String id;

    /**
     * Constructor for a graphql-ws 'error' message.
     *
     * @param id     The required non-empty id of the message.
     * @param errors The errors resulting from the specific subscription with the corresponding id.
     */
    @JsonCreator
    public ErrorMessage(@NonNull @JsonProperty("id") String id, @NonNull @JsonProperty("payload") List<Map<String, Object>> errors) {
        super(errors);
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("'id' is required for messages with type '" + getMessageType() + "'.");
        }
        this.id = id;
    }

    /**
     * Factory method for a graphql-ws 'error' message.
     *
     * @param id     The required non-empty id of the message.
     * @param errors The errors resulting from the specific subscription with the corresponding id.
     * @return A new error message.
     */
    public static ErrorMessage of(@NonNull String id, @NonNull List<GraphQLError> errors) {
        return new ErrorMessage(id, errors.stream().map(GraphQLError::toSpecification).collect(Collectors.toList()));
    }

    /**
     * Get the required non-empty message id.
     *
     * @return The message id.
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Get the required value of the message's <code>type</code> field.
     *
     * @return The message's type
     */
    @Override
    @JsonIgnore
    @NonNull
    String getMessageType() {
        return Types.ERROR;
    }
}

/**
 * A graphql-ws message for the completion of a subscription.
 */
final class CompleteMessage extends Message {

    @NonNull
    private final String id;

    /**
     * Constructor for a graphql-ws 'complete' message.
     *
     * @param id The required non-empty id of the message.
     */
    public CompleteMessage(@NonNull String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("'id' is required for messages with type '" + getMessageType() + "'.");
        }
        this.id = id;
    }

    /**
     * Get the required non-empty message id.
     *
     * @return The message id.
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Get the required value of the message's <code>type</code> field.
     *
     * @return The message's type
     */
    @Override
    @JsonIgnore
    @NonNull
    String getMessageType() {
        return Types.COMPLETE;
    }
}
