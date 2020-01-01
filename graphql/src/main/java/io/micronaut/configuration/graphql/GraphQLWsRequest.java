package io.micronaut.configuration.graphql;

import javax.annotation.Nullable;

/**
 * Class to handle the message to and from the websocket.
 *
 * @author Gerard Klijs
 * @since 1.3
 */
public class GraphQLWsRequest {

    private static final String TYPE_ERROR_MESSAGE = "Could not map %s to a known client type.";

    private ClientType type;
    @Nullable
    private String id;
    @Nullable
    private GraphQLRequestBody payload;

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
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Get the payload.
     *
     * @return payload as map, likely to contain a graphql query
     */
    public GraphQLRequestBody getPayload() {
        return payload;
    }

    /**
     * Sets the payload.
     *
     * @param payload the payload
     */
    public void setPayload(final GraphQLRequestBody payload) {
        this.payload = payload;
    }

    private ClientType fromString(String type) {
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
    enum ClientType {
        GQL_CONNECTION_INIT("connection_init"),
        GQL_START("start"),
        GQL_STOP("stop"),
        GQL_CONNECTION_TERMINATE("connection_terminate");

        private String type;

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
