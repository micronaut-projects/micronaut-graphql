package io.micronaut.configuration.graphql.apollo.ws

import io.micronaut.configuration.graphql.GraphQLRequestBody
import io.micronaut.configuration.graphql.GraphQLResponseBody
import io.micronaut.jackson.databind.JacksonDatabindMapper
import io.micronaut.json.JsonMapper
import spock.lang.Specification

class GraphQLApolloWsSerializationSpec extends Specification {

    void "test request serialization using jackson object mapper"() {
        given:
        GraphQLApolloWsRequest request = new GraphQLApolloWsRequest()
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_CONNECTION_INIT.getType())
        request.setId("test-id")
        GraphQLRequestBody body = new GraphQLRequestBody()
        body.setOperationName("test-operation")
        body.setQuery("test-query")
        body.setVariables(Map.of("test-key", "test-value"))
        request.setPayload(body)

        when:
        JsonMapper jsonMapper = new JacksonDatabindMapper();
        String serializedRequest = jsonMapper.writeValueAsString(request)

        then:
        serializedRequest == "{\"type\":\"connection_init\",\"id\":\"test-id\",\"payload\":{\"query\":\"test-query\",\"operationName\":\"test-operation\",\"variables\":{\"test-key\":\"test-value\"}}}"
    }

    void "test request deserialization using jackson object mapper"() {
        given:
        String request = "{\"type\":\"connection_init\",\"id\":\"test-id\",\"payload\":{\"query\":\"test-query\",\"operationName\":\"test-operation\",\"variables\":{\"test-key\":\"test-value\"}}}"

        when:
        JsonMapper jsonMapper = new JacksonDatabindMapper();
        GraphQLApolloWsRequest deserializedRequest = jsonMapper.readValue(request, GraphQLApolloWsRequest)

        then:
        deserializedRequest.getType() == GraphQLApolloWsRequest.ClientType.GQL_CONNECTION_INIT
        deserializedRequest.getId() == "test-id"
        deserializedRequest.getPayload().getQuery() == "test-query"
        deserializedRequest.getPayload().getOperationName() == "test-operation"
        deserializedRequest.getPayload().getVariables().size() == 1
        deserializedRequest.getPayload().getVariables().get("test-key") == "test-value"
    }

    void "test response serialization using jackson object mapper"() {
        given:
        GraphQLResponseBody body = new GraphQLResponseBody(Map.of("test-key", "test-value"))
        GraphQLApolloWsResponse response = new GraphQLApolloWsResponse(GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_ACK, "test-id", body)

        when:
        JsonMapper jsonMapper = new JacksonDatabindMapper();
        String serializedResponse = jsonMapper.writeValueAsString(response)

        then:
        serializedResponse == "{\"type\":\"connection_ack\",\"id\":\"test-id\",\"payload\":{\"test-key\":\"test-value\"}}"
    }

    void "test response deserialization using jackson object mapper"() {
        given:
        String response = "{\"type\":\"connection_ack\",\"id\":\"test-id\",\"payload\":{\"test-key\":\"test-value\"}}"

        when:
        JsonMapper jsonMapper = new JacksonDatabindMapper();
        GraphQLApolloWsResponse deserializedResponse = jsonMapper.readValue(response, GraphQLApolloWsResponse)

        then:
        deserializedResponse.getType() == GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_ACK.type
        deserializedResponse.getId() == "test-id"
        deserializedResponse.getPayload().getSpecification().size() == 1
        deserializedResponse.getPayload().getSpecification().get("test-key") == "test-value"
    }
}
