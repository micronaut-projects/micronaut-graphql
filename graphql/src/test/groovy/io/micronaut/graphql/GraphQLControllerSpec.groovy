/*
 * Copyright 2017-2018 original authors
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

package io.micronaut.graphql

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.GraphQL
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.env.Environment
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.Specification

import javax.annotation.Nullable
import javax.inject.Singleton
import java.util.concurrent.CompletableFuture

/**
 * @author Marcel Overdijk
 * @since 1.0
 */
class GraphQLControllerSpec extends Specification {

    @AutoCleanup
    EmbeddedServer embeddedServer

    GraphQL graphQL
    GraphQLClient client

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
        client = embeddedServer.applicationContext.getBean(GraphQLClient)
        executionInput = null
        1 * graphQL.executeAsync(_) >> { ExecutionInput executionInput ->
            this.executionInput = executionInput
            return executionResult
        }
    }

    void "test simple get"() {
        given:
        String query = "{ foo }"

        when:
        def response = client.get(query)

        then:
        response.status == HttpStatus.OK
        response.header(HttpHeaders.CONTENT_TYPE) == GraphQLController.APPLICATION_JSON_UTF8
        response.body()["data"] == "bar"

        and:
        executionInput.query == query
        executionInput.operationName == null
        executionInput.variables == [:]
    }

    void "test get with operation name"() {
        given:
        String query = "query myQuery { foo }"
        String operationName = "myQuery"

        when:
        def response = client.get(query, operationName, null)

        then:
        response.status == HttpStatus.OK
        response.header(HttpHeaders.CONTENT_TYPE) == GraphQLController.APPLICATION_JSON_UTF8
        response.body()["data"] == "bar"

        and:
        executionInput.query == query
        executionInput.operationName == operationName
        executionInput.variables == [:]
    }

    void "test get with operation name and variables"() {
        given:
        String query = "query myQuery { foo }"
        String operationName = "myQuery"
        String variables = '{"variable": "variableValue"}'

        when:
        def response = client.get(query, operationName, variables)

        then:
        response.status == HttpStatus.OK
        response.header(HttpHeaders.CONTENT_TYPE) == GraphQLController.APPLICATION_JSON_UTF8
        response.body()["data"] == "bar"

        and:
        executionInput.query == query
        executionInput.operationName == operationName
        executionInput.variables == ["variable": "variableValue"]
    }

    void "test simple post"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody()
        body.query = "{ foo }"

        when:
        def response = client.post(body)

        then:
        response.status == HttpStatus.OK
        response.header(HttpHeaders.CONTENT_TYPE) == GraphQLController.APPLICATION_JSON_UTF8
        response.body()["data"] == "bar"

        and:
        executionInput.query == body.query
        executionInput.operationName == null
        executionInput.variables == [:]
    }

    void "test post with operation name"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody()
        body.query = "query myQuery { foo }"
        body.operationName = "myQuery"

        when:
        def response = client.post(body)

        then:
        response.status == HttpStatus.OK
        response.header(HttpHeaders.CONTENT_TYPE) == GraphQLController.APPLICATION_JSON_UTF8
        response.body()["data"] == "bar"

        and:
        executionInput.query == body.query
        executionInput.operationName == body.operationName
        executionInput.variables == [:]
    }

    void "test post with operation name and variables"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody()
        body.query = "query myQuery { foo }"
        body.operationName = "myQuery"
        body.variables = ["variable": "variableValue"]

        when:
        def response = client.post(body)

        then:
        response.status == HttpStatus.OK
        response.header(HttpHeaders.CONTENT_TYPE) == GraphQLController.APPLICATION_JSON_UTF8
        response.body()["data"] == "bar"

        and:
        executionInput.query == body.query
        executionInput.operationName == body.operationName
        executionInput.variables == body.variables
    }

    @Client("/graphql")
    static interface GraphQLClient {

        @Get("{?query}")
        HttpResponse<Map> get(@QueryValue String query)

        @Get("{?query,operationName,variables}")
        HttpResponse<Map> get(@QueryValue String query, @QueryValue @Nullable String operationName, @QueryValue @Nullable String variables)

        @Post
        HttpResponse<Map> post(@Body GraphQLRequestBody body)
    }

    @Factory
    static class GraphQLFactory {

        @Bean
        @Singleton
        GraphQL graphQL() {
            graphQL
        }
    }
}
