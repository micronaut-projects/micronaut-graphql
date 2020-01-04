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

package io.micronaut.configuration.graphql

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.*
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.websocket.RxWebSocketClient
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import spock.lang.AutoCleanup
import spock.lang.Specification

import javax.inject.Singleton
import java.util.concurrent.TimeUnit

/**
 * @author Gerard Klijs
 * @since 1.3
 */
class GraphQLWsControllerSpec extends Specification {

    @AutoCleanup
    EmbeddedServer embeddedServer

    GraphQLWsClient graphQLWsClient

    def setup() {
        embeddedServer = ApplicationContext.run(
                EmbeddedServer,
                ["spec.name": GraphQLWsControllerSpec.simpleName],
                "websocket") as EmbeddedServer
        RxWebSocketClient wsClient = embeddedServer.applicationContext.createBean(RxWebSocketClient, embeddedServer.getURI())
        graphQLWsClient = wsClient.connect(GraphQLWsClient, "/graphql-ws").blockingFirst();
    }

    void "test init connection"() {
        given:
        GraphQLWsRequest request = new GraphQLWsRequest()
        request.setType(GraphQLWsRequest.ClientType.GQL_CONNECTION_INIT.getType())

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLWsResponse response = graphQLWsClient.nextResponse()
        response.getType() == GraphQLWsResponse.ServerType.GQL_CONNECTION_ACK.getType()

        and:
        response.id == null
        response.payload == null
    }

    void "test query over websocket"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody();
        body.query = "query{ foo }"
        GraphQLWsRequest request = new GraphQLWsRequest()
        request.setType(GraphQLWsRequest.ClientType.GQL_START.type)
        request.setId("foo_id")
        request.setPayload(body)

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLWsResponse response = graphQLWsClient.nextResponse()
        response.getPayload().getSpecification().get("data") == [foo: "bar"]
        GraphQLWsResponse completeResponse = graphQLWsClient.nextResponse()

        and:
        response.id == "foo_id"
        response.type == "data"
        completeResponse.type == "complete"
    }

    void "handle error in query over websocket"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody();
        body.query = "query{ error }"
        GraphQLWsRequest request = new GraphQLWsRequest()
        request.setType(GraphQLWsRequest.ClientType.GQL_START.type)
        request.setId("error_id")
        request.setPayload(body)

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLWsResponse response = graphQLWsClient.nextResponse()
        response.getPayload().getSpecification().get("data") == null
        response.getPayload().getSpecification().get("errors") != null
        List<Map> errorList = (List<Map>) response.getPayload().getSpecification().get("errors")
        errorList != null
        errorList.size() == 1
        errorList.get(0).get("message") == "Exception while fetching data (/error) : No error present"
        errorList.get(0).get("locations") == [[line: 1, column: 8]]
        errorList.get(0).get("path") == ["error"]
        errorList.get(0).get("extensions") == [classification: "DataFetchingException"]
        GraphQLWsResponse completeResponse = graphQLWsClient.nextResponse()

        and:
        response.id == "error_id"
        response.type == "error"
        completeResponse.type == "complete"
    }

    void "test mutation over websocket"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody();
        body.query = "mutation{ change( newValue: \"Value_B\" ){ current old }}"
        GraphQLWsRequest request = new GraphQLWsRequest()
        request.setType(GraphQLWsRequest.ClientType.GQL_START.type)
        request.setId("change_id")
        request.setPayload(body)

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLWsResponse response = graphQLWsClient.nextResponse()
        response.getPayload().getSpecification().get("data") == [change: [current: "Value_B", old: ["Value_A"]]]
        GraphQLWsResponse completeResponse = graphQLWsClient.nextResponse()

        and:
        response.id == "change_id"
        response.type == "data"
        completeResponse.type == "complete"
    }

    void "test customizer using http request in mutation over websocket"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody();
        body.query = "mutation{ change( newValue: \"\$[path]\" ){ current old }}"
        GraphQLWsRequest request = new GraphQLWsRequest()
        request.setType(GraphQLWsRequest.ClientType.GQL_START.type)
        request.setId("change_id")
        request.setPayload(body)

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLWsResponse response = graphQLWsClient.nextResponse()
        response.getPayload().getSpecification().get("data") == [change: [current: "/graphql-ws", old: ["Value_A"]]]
        GraphQLWsResponse completeResponse = graphQLWsClient.nextResponse()

        and:
        response.id == "change_id"
        response.type == "data"
        completeResponse.type == "complete"
    }

    void "test subscription over websocket, stop after two"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody()
        body.query = "subscription{ counter }"
        GraphQLWsRequest request = new GraphQLWsRequest()
        request.setType(GraphQLWsRequest.ClientType.GQL_START.type)
        request.setId("counter_id")
        request.setPayload(body)

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLWsResponse response1 = graphQLWsClient.nextResponse()
        response1.getPayload().getSpecification().get("data") == [counter: 0]
        GraphQLWsResponse response2 = graphQLWsClient.nextResponse()
        response2.getPayload().getSpecification().get("data") == [counter: 1]
        request.setType(GraphQLWsRequest.ClientType.GQL_STOP.type)
        graphQLWsClient.send(request)
        GraphQLWsResponse response3 = graphQLWsClient.nextResponse()

        and:
        response1.id == "counter_id"
        response1.type == "data"
        response2.id == "counter_id"
        response2.type == "data"
        response3.id == "counter_id"
        response3.type == "complete"
    }

    void "test subscription over websocket, let it complete"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody();
        body.query = "subscription{ counter }"
        GraphQLWsRequest request = new GraphQLWsRequest()
        request.setType(GraphQLWsRequest.ClientType.GQL_START.type)
        request.setId("counter_id")
        request.setPayload(body)

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLWsResponse response1 = graphQLWsClient.nextResponse()
        response1.getPayload().getSpecification().get("data") == [counter: 0]
        GraphQLWsResponse response2 = graphQLWsClient.nextResponse()
        response2.getPayload().getSpecification().get("data") == [counter: 1]
        GraphQLWsResponse response3 = graphQLWsClient.nextResponse()
        response3.getPayload().getSpecification().get("data") == [counter: 2]
        GraphQLWsResponse response4 = graphQLWsClient.nextResponse()

        and:
        response1.id == "counter_id"
        response1.type == "data"
        response2.id == "counter_id"
        response2.type == "data"
        response3.id == "counter_id"
        response3.type == "data"
        response4.id == "counter_id"
        response4.type == "complete"
    }
}

@Singleton
@Primary
@Requires(property = "spec.name", value = "GraphQLWsControllerSpec")
class SetValueFromRequestInputCustomizer implements GraphQLExecutionInputCustomizer {
    private final static String PATH_PLACEHOLDER = "\$[path]"

    @Override
    Publisher<ExecutionInput> customize(ExecutionInput executionInput, HttpRequest httpRequest) {
        if (executionInput.getQuery().contains(PATH_PLACEHOLDER)) {
            return Publishers.just(executionInput.transform({
                builder -> builder.query(executionInput.getQuery().replace(PATH_PLACEHOLDER, httpRequest.getPath()))
            }))
        } else {
            return Publishers.just(executionInput)
        }
    }
}

@Factory
class GraphQLWsFactory {

    @Bean
    @Singleton
    @Requires(property = "spec.name", value = "GraphQLWsControllerSpec")
    GraphQL graphQL() {
        SchemaParser schemaParser = new SchemaParser()
        SchemaGenerator schemaGenerator = new SchemaGenerator()

        TypeDefinitionRegistry typeRegistry = schemaParser.parse(new InputStreamReader(
                getClass().getResourceAsStream("/websocket.graphql")))

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type(TypeRuntimeWiring.newTypeWiring("QueryRoot").dataFetcher("foo", new Foo()))
                .type(TypeRuntimeWiring.newTypeWiring("QueryRoot").dataFetcher("error", new Error()))
                .type(TypeRuntimeWiring.newTypeWiring("MutationRoot").dataFetcher("change", new Change()))
                .type(TypeRuntimeWiring.newTypeWiring("SubscriptionRoot").dataFetcher("counter", new Counter()))
                .build()

        return GraphQL
                .newGraphQL(schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring))
                .build()
    }
}

class Foo implements DataFetcher<String> {
    @Override
    String get(DataFetchingEnvironment environment) throws Exception {
        return "bar"
    }
}

class Error implements DataFetcher<String> {
    @Override
    String get(DataFetchingEnvironment environment) throws Exception {
        throw new InstantiationException("No error present")
    }
}

class Change implements DataFetcher<Values> {
    private Values values = new Values()

    @Override
    Values get(DataFetchingEnvironment environment) throws Exception {
        String newValue = environment.getArgument("newValue")
        return values.change(newValue)
    }

    class Values {
        private List<String> old = []
        private String current = "Value_A"

        List<String> getOld() {
            return old
        }

        String getCurrent() {
            return current
        }

        Values change(String current) {
            old.add(this.current)
            this.current = current
            return this
        }
    }
}

class Counter implements DataFetcher<Publisher<Integer>> {
    @Override
    Publisher<Integer> get(DataFetchingEnvironment environment) throws Exception {
        return Flowable.range(0, 3)
                .delay(1L, TimeUnit.SECONDS)
    }
}
