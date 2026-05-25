/*
 * Copyright 2017-2019 original authors
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

package io.micronaut.configuration.graphql

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.GraphQL
import graphql.GraphQLContext
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.cookie.Cookie
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static io.micronaut.http.MediaType.APPLICATION_GRAPHQL
import static io.micronaut.http.MediaType.APPLICATION_JSON

/**
 * @author Marcel Overdijk
 * @since 1.0
 */
class GraphQLControllerSpec extends Specification {

    @AutoCleanup
    EmbeddedServer embeddedServer

    GraphQL graphQL
    GraphQLClient graphQLClient
    @AutoCleanup
    HttpClient httpClient

    ExecutionInput executionInput
    List<ExecutionInput> executionInputs

    CompletableFuture<ExecutionResult> executionResult = CompletableFuture.completedFuture(
            ExecutionResultImpl.newExecutionResult()
                    .data("bar")
                    .build())

    def setup() {
        graphQL = Mock()
        embeddedServer = ApplicationContext.run(
                EmbeddedServer,
                ["spec.name": GraphQLControllerSpec.simpleName],
                Environment.TEST)
        embeddedServer.applicationContext.registerSingleton(GraphQL, graphQL)
        graphQLClient = embeddedServer.applicationContext.getBean(GraphQLClient)
        httpClient = embeddedServer.applicationContext.createBean(HttpClient, embeddedServer.getURL())
        executionInput = null
        executionInputs = []
        _ * graphQL.executeAsync(_) >> { ExecutionInput executionInput ->
            this.executionInput = executionInput
            this.executionInputs << executionInput
            if (executionInput.query == "{ testHeaders }") {
                GraphQLContext graphQlContext = executionInput.getGraphQLContext()

                MutableHttpResponse httpResponse = graphQlContext.get("httpResponse")

                assert httpResponse: "HTTP response can not be null"

                httpResponse.header("X-Foo", "bar")
                httpResponse.cookie(Cookie.of("foo", "bar"))
            }
            return executionResult
        }
    }

    void "test get with query parameter"() {
        given:
        String query = "{ foo }"

        when:
        GraphQLResponseBody response = graphQLClient.get(query, null, null)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInputs.size() == 1
        executionInput.query == query
        executionInput.operationName == null
        executionInput.variables == [:]
    }

    void "test get with query and operation name parameters"() {
        given:
        String query = "query myQuery { foo }"
        String operationName = "myQuery"

        when:
        GraphQLResponseBody response = graphQLClient.get(query, operationName, null)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInputs.size() == 1
        executionInput.query == query
        executionInput.operationName == operationName
        executionInput.variables == [:]
    }

    void "test get with query, operation name and variables parameters"() {
        given:
        String query = "query myQuery { foo }"
        String operationName = "myQuery"
        String variables = '{"variable": "variableValue"}'

        when:
        GraphQLResponseBody response = graphQLClient.get(query, operationName, variables)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInputs.size() == 1
        executionInput.query == query
        executionInput.operationName == operationName
        executionInput.variables == ["variable": "variableValue"]
    }

    void "test post with query parameter"() {
        given:
        String query = "{ foo }"

        when:
        GraphQLResponseBody response = graphQLClient.post(query, null, null)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInputs.size() == 1
        executionInput.query == query
        executionInput.operationName == null
        executionInput.variables == [:]
    }

    void "test post with query and operation name parameters"() {
        given:
        String query = "query myQuery { foo }"
        String operationName = "myQuery"

        when:
        GraphQLResponseBody response = graphQLClient.post(query, operationName, null)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInputs.size() == 1
        executionInput.query == query
        executionInput.operationName == operationName
        executionInput.variables == [:]
    }

    void "test post with query, operation name and variables parameters"() {
        given:
        String query = "query myQuery { foo }"
        String operationName = "myQuery"
        String variables = '{"variable": "variableValue"}'

        when:
        GraphQLResponseBody response = graphQLClient.post(query, operationName, variables)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInputs.size() == 1
        executionInput.query == query
        executionInput.operationName == operationName
        executionInput.variables == ["variable": "variableValue"]
    }

    void "test post with application/json body with query json field"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody()
        body.query = "{ foo }"

        when:
        GraphQLResponseBody response = graphQLClient.post(body)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInputs.size() == 1
        executionInput.query == body.query
        executionInput.operationName == null
        executionInput.variables == [:]
    }

    void "test post with application/json body with query and operation name json fields"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody()
        body.query = "query myQuery { foo }"
        body.operationName = "myQuery"

        when:
        GraphQLResponseBody response = graphQLClient.post(body)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInputs.size() == 1
        executionInput.query == body.query
        executionInput.operationName == body.operationName
        executionInput.variables == [:]
    }

    void "test post with application/json body with query, operation name and variables json fields"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody()
        body.query = "query myQuery { foo }"
        body.operationName = "myQuery"
        body.variables = ["variable": "variableValue"]

        when:
        GraphQLResponseBody response = graphQLClient.post(body)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInputs.size() == 1
        executionInput.query == body.query
        executionInput.operationName == body.operationName
        executionInput.variables == body.variables
    }

    void "test post with application/json batch body"() {
        given:
        String body = '''
[
  {"query":"{ foo }"},
  {"query":"query myQuery { foo }","operationName":"myQuery","variables":{"variable":"variableValue"}}
]
'''

        when:
        HttpResponse<List<GraphQLResponseBody>> response = httpClient.toBlocking().exchange(
                io.micronaut.http.HttpRequest.POST("/graphql", body).contentType(APPLICATION_JSON),
                Argument.listOf(GraphQLResponseBody))
        List<GraphQLResponseBody> batchResponse = response.body()

        then:
        response.status() == HttpStatus.OK
        batchResponse*.specification*.data == ["bar", "bar"]

        and:
        executionInputs.size() == 2
        executionInputs*.query == ["{ foo }", "query myQuery { foo }"]
        executionInputs*.operationName == [null, "myQuery"]
        executionInputs*.variables == [[:], ["variable": "variableValue"]]
    }

    void "test post with application/graphql body"() {
        given:
        String body = "{ foo }"

        when:
        GraphQLResponseBody response = graphQLClient.post(body)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInputs.size() == 1
        executionInput.query == body
        executionInput.operationName == null
        executionInput.variables == [:]
    }

    void "test additional headers and cookies"() {
        given:
        String body = "{ testHeaders }"

        when:
        HttpResponse httpResponse = graphQLClient.postWithResponse(body)

        then:
        httpResponse.status() == HttpStatus.OK
        httpResponse.body().getSpecification()["data"] == "bar"
        httpResponse.header("X-Foo") == "bar"
        httpResponse.header("set-cookie") == "foo=bar"

        and:
        executionInputs.size() == 1
    }

    void "test get accepts application wildcard accept header"() {
        given:
        String query = "{ foo }"

        when:
        HttpResponse<GraphQLResponseBody> response = graphQLClient.getWithResponse(query, null, null, 'application/*')

        then:
        response.status() == HttpStatus.OK
        response.contentType.get() == MediaType.APPLICATION_JSON_TYPE
        response.body().getSpecification()["data"] == "bar"
    }

    void "test post accepts application wildcard accept header"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody()
        body.query = "{ foo }"

        when:
        HttpResponse<GraphQLResponseBody> response = graphQLClient.postJsonWithAccept(body, 'application/*')

        then:
        response.status() == HttpStatus.OK
        response.contentType.get() == MediaType.APPLICATION_JSON_TYPE
        response.body().getSpecification()["data"] == "bar"
    }

    void "test post with malformed application json body returns bad request"() {
        when:
        graphQLClient.postMalformedJson('Bad request')

        then:
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.BAD_REQUEST
    }

    void "test get with malformed variables returns bad request"() {
        when:
        graphQLClient.get('{ foo }', null, '{invalid json}')

        then:
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.BAD_REQUEST
    }

    void "test post with malformed variables returns bad request"() {
        when:
        graphQLClient.post('{ foo }', null, '{invalid json}')

        then:
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.BAD_REQUEST
    }

    @Client("/graphql")
    static interface GraphQLClient {

        @Get("{?query,operationName,variables}")
        GraphQLResponseBody get(@QueryValue String query, @QueryValue @Nullable String operationName, @QueryValue @Nullable String variables)

        @Get(value = "{?query,operationName,variables}", processes = 'application/*')
        HttpResponse<GraphQLResponseBody> getWithResponse(@QueryValue String query, @QueryValue @Nullable String operationName, @QueryValue @Nullable String variables, @io.micronaut.http.annotation.Header String accept)

        @Post(value = "{?query,operationName,variables}")
        GraphQLResponseBody post(@QueryValue String query, @QueryValue @Nullable String operationName, @QueryValue @Nullable String variables)

        @Post(produces = APPLICATION_JSON)
        GraphQLResponseBody post(@Body GraphQLRequestBody body)

        @Post(produces = APPLICATION_JSON, processes = 'application/*')
        HttpResponse<GraphQLResponseBody> postJsonWithAccept(@Body GraphQLRequestBody body, @io.micronaut.http.annotation.Header String accept)

        @Post(produces = APPLICATION_GRAPHQL)
        GraphQLResponseBody post(@Body String body)

        @Post(produces = APPLICATION_GRAPHQL)
        HttpResponse<GraphQLResponseBody> postWithResponse(@Body String body)

        @Post(consumes = APPLICATION_JSON)
        GraphQLResponseBody postMalformedJson(@Body String body)

    }

    @Factory
    static class GraphQLFactory {

        @Bean
        @Singleton
        @Requires(property = "spec.name", value = "GraphQLControllerSpec")
        GraphQL graphQL() {
            graphQL
        }
    }
}

@Singleton
@Primary
@Requires(property = "spec.name", value = "GraphQLControllerSpec")
class SetRequestResponseInputCustomizer implements GraphQLExecutionInputCustomizer {

    @Override
    Publisher<ExecutionInput> customize(ExecutionInput executionInput, HttpRequest httpRequest,
                                        MutableHttpResponse<String> httpResponse) {
        GraphQLContext graphQLContext = executionInput.getGraphQLContext();
        graphQLContext.put("httpRequest", httpRequest);
        graphQLContext.put("httpResponse", httpResponse);
        return Publishers.just(executionInput);
    }
}
