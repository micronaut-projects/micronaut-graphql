package io.micronaut.configuration.graphql.ws;

import io.micronaut.configuration.graphql.GraphQLRequestBody;

/**
 * A class that represents an GQL_START message (and any other except GQL_CONNECTION_INIT)
 *
 * @since 4.0
 * @author Nick Hensel
 */
public class GraphQLWsStartRequest extends GraphQLWsRequest<GraphQLRequestBody> {
}
