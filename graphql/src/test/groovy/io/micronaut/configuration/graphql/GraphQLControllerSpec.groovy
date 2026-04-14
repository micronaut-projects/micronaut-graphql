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
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.multipart.CompletedFileUpload
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

    @AutoCleanup
    HttpClient httpClient

    GraphQL graphQL
    GraphQLClient graphQLClient

    ExecutionInput executionInput
    String uploadedPartName
    String uploadedFilename
    byte[] uploadedBytes

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
        httpClient = embeddedServer.applicationContext.createBean(HttpClient, embeddedServer.getURL())
        graphQLClient = embeddedServer.applicationContext.getBean(GraphQLClient)
        executionInput = null
        graphQL.executeAsync(_) >> { ExecutionInput executionInput ->
        uploadedPartName = null
        uploadedFilename = null
        uploadedBytes = null
        graphQL.executeAsync(_) >> { ExecutionInput executionInput ->
            this.executionInput = executionInput
            def uploadedFiles = executionInput.variables?.input?.files
            if (uploadedFiles instanceof List && uploadedFiles[0] instanceof CompletedFileUpload) {
                CompletedFileUpload file = uploadedFiles[0] as CompletedFileUpload
                uploadedPartName = file.name
                uploadedFilename = file.filename
                uploadedBytes = file.bytes
            }
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

    void "test post with multipart form data body"() {
        given:
        String query = "mutation (\$input: UploadInput!) { upload(input: \$input) }"
        MultipartBody body = MultipartBody.builder()
                .addPart("operations", """{"query":"${query}","variables":{"input":{"files":[null]}}}""")
                .addPart("map", '{"0":["variables.input.files.0"]}')
                .addPart("0", "upload.txt", MediaType.TEXT_PLAIN_TYPE, "file-body".bytes)
                .build()
        HttpRequest<?> request = HttpRequest.POST("/graphql", body)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        when:
        GraphQLResponseBody response = httpClient.toBlocking().retrieve(request, GraphQLResponseBody)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInput.query == query
        executionInput.operationName == null
        executionInput.variables.input.files[0] instanceof CompletedFileUpload

        and:
        uploadedPartName == "0"
        uploadedFilename == "upload.txt"
        uploadedBytes == "file-body".bytes
    }

    void "test post with multipart form data body defaults null query and variables"() {
        given:
        MultipartBody body = MultipartBody.builder()
                .addPart("operations", '{"query":null,"variables":null}')
                .addPart("map", '{}')
                .build()
        HttpRequest<?> request = HttpRequest.POST("/graphql", body)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        when:
        GraphQLResponseBody response = httpClient.toBlocking().retrieve(request, GraphQLResponseBody)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInput.query == ""
        executionInput.operationName == null
        executionInput.variables == [:]
    }

    void "test post with multipart form data body can map one upload to multiple variables"() {
        given:
        String query = "mutation (\$input: UploadInput!) { upload(input: \$input) }"
        MultipartBody body = MultipartBody.builder()
                .addPart("operations", """{"query":"${query}","variables":{"input":{"files":[null,null]}}}""")
                .addPart("map", '{"0":["variables.input.files.0","variables.input.files.1"]}')
                .addPart("0", "upload.txt", MediaType.TEXT_PLAIN_TYPE, "file-body".bytes)
                .build()
        HttpRequest<?> request = HttpRequest.POST("/graphql", body)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        when:
        GraphQLResponseBody response = httpClient.toBlocking().retrieve(request, GraphQLResponseBody)

        then:
        response.getSpecification()["data"] == "bar"

        and:
        executionInput.query == query
        executionInput.operationName == null
        executionInput.variables.input.files[0] instanceof CompletedFileUpload
        executionInput.variables.input.files[1] instanceof CompletedFileUpload
        executionInput.variables.input.files[0].filename == "upload.txt"
        executionInput.variables.input.files[1].filename == "upload.txt"

        and:
        uploadedPartName == "0"
        uploadedFilename == "upload.txt"
        uploadedBytes == "file-body".bytes
    }

    void "test post with multipart form data body requires operations part"() {
        given:
        MultipartBody body = MultipartBody.builder()
                .addPart("map", '{}')
                .build()
        HttpRequest<?> request = HttpRequest.POST("/graphql", body)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        when:
        httpClient.toBlocking().exchange(request, String)

        then:
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.UNPROCESSABLE_ENTITY
        e.response.getBody(String).orElse("").contains("Missing multipart field: operations")
        executionInput == null
    }

    void "test post with multipart form data body rejects invalid mapping payload"() {
        given:
        MultipartBody body = MultipartBody.builder()
                .addPart("operations", '{"query":"","variables":{"input":{"files":[null]}}}')
                .addPart("map", '{"0":[1]}')
                .addPart("0", "upload.txt", MediaType.TEXT_PLAIN_TYPE, "file-body".bytes)
                .build()
        HttpRequest<?> request = HttpRequest.POST("/graphql", body)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        when:
        httpClient.toBlocking().exchange(request, String)

        then:
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.UNPROCESSABLE_ENTITY
        e.response.getBody(String).orElse("").contains("Invalid multipart mapping payload")
        executionInput == null
    }

    void "test post with multipart form data body rejects unsupported variable path"() {
        given:
        MultipartBody body = MultipartBody.builder()
                .addPart("operations", '{"query":"","variables":{"input":{"files":[null]}}}')
                .addPart("map", '{"0":["input.files.0"]}')
                .addPart("0", "upload.txt", MediaType.TEXT_PLAIN_TYPE, "file-body".bytes)
                .build()
        HttpRequest<?> request = HttpRequest.POST("/graphql", body)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        when:
        httpClient.toBlocking().exchange(request, String)

        then:
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.UNPROCESSABLE_ENTITY
        e.response.getBody(String).orElse("").contains("Unsupported multipart variable path: input.files.0")
        executionInput == null
    }

    void "test post with multipart form data body rejects unknown list index"() {
        given:
        MultipartBody body = MultipartBody.builder()
                .addPart("operations", '{"query":"","variables":{"input":{"files":[null]}}}')
                .addPart("map", '{"0":["variables.input.files.1"]}')
                .addPart("0", "upload.txt", MediaType.TEXT_PLAIN_TYPE, "file-body".bytes)
                .build()
        HttpRequest<?> request = HttpRequest.POST("/graphql", body)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        when:
        httpClient.toBlocking().exchange(request, String)

        then:
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.UNPROCESSABLE_ENTITY
        e.response.getBody(String).orElse("").contains("Unknown multipart variable path: variables.input.files.1")
        executionInput == null
    }

    void "test post with multipart form data body requires map part"() {
        given:
        MultipartBody body = MultipartBody.builder()
                .addPart("operations", '{"query":"","variables":{}}')
                .build()
        HttpRequest<?> request = HttpRequest.POST("/graphql", body)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        when:
        httpClient.toBlocking().exchange(request, String)

        then:
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.UNPROCESSABLE_ENTITY
        e.response.getBody(String).orElse("").contains("Missing multipart field: map")
        executionInput == null
    }

    void "test post with multipart form data body rejects non-file upload parts"() {
        given:
        MultipartBody body = MultipartBody.builder()
                .addPart("operations", '{"query":"","variables":{"input":null}}')
                .addPart("map", '{"textField":["variables.input"]}')
                .addPart("textField", "not a file")
                .build()
        HttpRequest<?> request = HttpRequest.POST("/graphql", body)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        when:
        httpClient.toBlocking().exchange(request, String)

        then:
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.UNPROCESSABLE_ENTITY
        e.response.getBody(String).orElse("").contains("Multipart field is not a file upload: textField")
        executionInput == null
    }

    void "test post with multipart form data body rejects non-null variable target"() {
        given:
        MultipartBody body = MultipartBody.builder()
                .addPart("operations", '{"query":"","variables":{"input":"existing"}}')
                .addPart("map", '{"0":["variables.input"]}')
                .addPart("0", "upload.txt", MediaType.TEXT_PLAIN_TYPE, "file-body".bytes)
                .build()
        HttpRequest<?> request = HttpRequest.POST("/graphql", body)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        when:
        httpClient.toBlocking().exchange(request, String)

        then:
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.UNPROCESSABLE_ENTITY
        e.response.getBody(String).orElse("").contains("Invalid multipart variable path: variables.input")
        executionInput == null
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

        @Post(value = "{?query,operationName,variables}")
        GraphQLResponseBody post(@QueryValue String query, @QueryValue @Nullable String operationName, @QueryValue @Nullable String variables)

        @Post(produces = APPLICATION_JSON)
        GraphQLResponseBody post(@Body GraphQLRequestBody body)

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
