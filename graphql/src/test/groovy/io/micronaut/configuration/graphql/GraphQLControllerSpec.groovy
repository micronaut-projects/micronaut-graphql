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
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.cookie.Cookie
import io.micronaut.runtime.server.EmbeddedServer
import org.reactivestreams.Publisher
import spock.lang.AutoCleanup
import spock.lang.Specification

import javax.inject.Singleton
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

    ExecutionInput executionInput

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
        embeddedServer.applicationContext.registerSingleton(graphQL)
        graphQLClient = embeddedServer.applicationContext.getBean(GraphQLClient)
        executionInput = null
        1 * graphQL.executeAsync(_) >> { ExecutionInput executionInput ->
            this.executionInput = executionInput
            if (executionInput.query == "{ testHeaders }") {
                GraphQLContext graphQlContext = executionInput.getContext()

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
        executionInput.query == body.query
        executionInput.operationName == body.operationName
        executionInput.variables == body.variables
    }

    void "test post with application/graphql body"() {
        given:
        String body = "{ foo }"

        when:
        GraphQLResponseBody response = graphQLClient.post(body)

        then:
        response.getSpecification()["data"] == "bar"

        and:
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
    }

    @Client("/graphql")
    static interface GraphQLClient {

        @Get("{?query,operationName,variables}")
        GraphQLResponseBody get(@QueryValue String query, @QueryValue @Nullable String operationName, @QueryValue @Nullable String variables)

        @Post(value = "{?query,operationName,variables}")
        GraphQLResponseBody post(@QueryValue String query, @QueryValue @Nullable String operationName, @QueryValue @Nullable String variables)

        @Post(produces = APPLICATION_JSON)
        GraphQLResponseBody post(@Body GraphQLRequestBody body)

        @Post(produces = APPLICATION_GRAPHQL)
        GraphQLResponseBody post(@Body String body)

        @Post(produces = APPLICATION_GRAPHQL)
        HttpResponse<GraphQLResponseBody> postWithResponse(@Body String body)

    }

    @Factory
    class GraphQLFactory {

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
        GraphQLContext graphQLContext = (GraphQLContext) executionInput.getContext();
        graphQLContext.put("httpRequest", httpRequest);
        graphQLContext.put("httpResponse", httpResponse);
        return Publishers.just(executionInput);
    }
}
