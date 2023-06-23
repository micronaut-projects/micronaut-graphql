package io.micronaut.configuration.graphql.apollo.ws

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
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

        when:
        String serializedRequest = objectMapper.writeValueAsString(request)

        then:
        serializedRequest == "{\"type\":\"connection_init\"}"
    }

    void "test request deserialization"() {
        given:
        String request = "{\"type\":\"connection_init\"}"

        when:
        GraphQLApolloWsRequest deserializedRequest = objectMapper.readValue(request, GraphQLApolloWsRequest)

        then:
        GraphQLApolloWsRequest.ClientType.GQL_CONNECTION_INIT == deserializedRequest.getType()
    }

    void "test response serialization"() {
        given:
        GraphQLApolloWsResponse response = new GraphQLApolloWsResponse(GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_ACK)

        when:
        String serializedResponse = objectMapper.writeValueAsString(response)

        then:
        serializedResponse == "{\"type\":\"connection_ack\"}"
    }

    void "test response deserialization"() {
        given:
        String response = "{\"type\":\"connection_ack\"}"

        when:
        GraphQLApolloWsResponse deserializedResponse = objectMapper.readValue(response, GraphQLApolloWsResponse)

        then:
        GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_ACK.type == deserializedResponse.getType()
    }
}
