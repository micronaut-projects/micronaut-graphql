package io.micronaut.configuration.graphql.apollo.ws

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.configuration.graphql.GraphQLRequestBody
import io.micronaut.configuration.graphql.GraphQLResponseBody
import io.micronaut.jackson.JacksonConfiguration
import io.micronaut.jackson.ObjectMapperFactory
import spock.lang.Shared
import spock.lang.Specification

class GraphQLApolloWsSerializationSpec extends Specification {

    @Shared
    ObjectMapper objectMapper

    def setupSpec() {
        JacksonConfiguration jacksonConfiguration = new JacksonConfiguration()
        ObjectMapperFactory objectMapperFactory = new ObjectMapperFactory()
        JsonFactory jsonFactory = objectMapperFactory.jsonFactory(jacksonConfiguration)
        objectMapper = objectMapperFactory.objectMapper(jacksonConfiguration, jsonFactory)
    }

    void "test request serialization"() {
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
        String serializedRequest = objectMapper.writeValueAsString(request)

        then:
        serializedRequest == "{\"type\":\"connection_init\",\"id\":\"test-id\",\"payload\":{\"query\":\"test-query\",\"operationName\":\"test-operation\",\"variables\":{\"test-key\":\"test-value\"}}}"
    }

    void "test request deserialization"() {
        given:
        String request = "{\"type\":\"connection_init\",\"id\":\"test-id\",\"payload\":{\"query\":\"test-query\",\"operationName\":\"test-operation\",\"variables\":{\"test-key\":\"test-value\"}}}"

        when:
        GraphQLApolloWsRequest deserializedRequest = objectMapper.readValue(request, GraphQLApolloWsRequest)

        then:
        deserializedRequest.getType() == GraphQLApolloWsRequest.ClientType.GQL_CONNECTION_INIT
        deserializedRequest.getId() == "test-id"
        deserializedRequest.getPayload().getQuery() == "test-query"
        deserializedRequest.getPayload().getOperationName() == "test-operation"
        deserializedRequest.getPayload().getVariables().size() == 1
        deserializedRequest.getPayload().getVariables().get("test-key") == "test-value"
    }

    void "test response serialization"() {
        given:
        GraphQLResponseBody body = new GraphQLResponseBody(Map.of("test-key", "test-value"))
        GraphQLApolloWsResponse response = new GraphQLApolloWsResponse(GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_ACK, "test-id", body)

        when:
        String serializedResponse = objectMapper.writeValueAsString(response)

        then:
        serializedResponse == "{\"type\":\"connection_ack\",\"id\":\"test-id\",\"payload\":{\"test-key\":\"test-value\"}}"
    }

    void "test response deserialization"() {
        given:
        String response = "{\"type\":\"connection_ack\",\"id\":\"test-id\",\"payload\":{\"test-key\":\"test-value\"}}"

        when:
        GraphQLApolloWsResponse deserializedResponse = objectMapper.readValue(response, GraphQLApolloWsResponse)

        then:
        deserializedResponse.getType() == GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_ACK.type
        deserializedResponse.getId() == "test-id"
        deserializedResponse.getPayload().getSpecification().size() == 1
        deserializedResponse.getPayload().getSpecification().get("test-key") == "test-value"
    }
}
