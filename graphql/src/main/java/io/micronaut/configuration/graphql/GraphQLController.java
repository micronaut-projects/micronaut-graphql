/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.configuration.graphql;

import graphql.ExecutionResult;
import org.jspecify.annotations.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.exceptions.HttpStatusException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static io.micronaut.http.MediaType.ALL;
import static io.micronaut.http.MediaType.APPLICATION_GRAPHQL_TYPE;
import static io.micronaut.http.MediaType.APPLICATION_JSON;
import static io.micronaut.http.MediaType.APPLICATION_JSON_TYPE;

/**
 * The GraphQL controller handling GraphQL requests.
 *
 * @author Marcel Overdijk
 * @author James Kleeh
 * @author Alexey Zhokhov
 * @since 1.0
 */
@Controller("${" + GraphQLConfiguration.PATH_CONFIG + ":" + GraphQLConfiguration.DEFAULT_PATH + "}")
public class GraphQLController {

    private final GraphQLInvocation graphQLInvocation;
    private final GraphQLExecutionResultHandler graphQLExecutionResultHandler;
    private final GraphQLJsonSerializer graphQLJsonSerializer;

    /**
     * Default constructor.
     *
     * @param graphQLInvocation             the {@link GraphQLInvocation} instance
     * @param graphQLExecutionResultHandler the {@link GraphQLExecutionResultHandler} instance
     * @param graphQLJsonSerializer         the {@link GraphQLJsonSerializer} instance
     */
    public GraphQLController(
            GraphQLInvocation graphQLInvocation,
            GraphQLExecutionResultHandler graphQLExecutionResultHandler,
            GraphQLJsonSerializer graphQLJsonSerializer) {
        this.graphQLInvocation = graphQLInvocation;
        this.graphQLExecutionResultHandler = graphQLExecutionResultHandler;
        this.graphQLJsonSerializer = graphQLJsonSerializer;
    }

    /**
     * Handles GraphQL {@code GET} requests.
     *
     * @param query         the GraphQL query
     * @param operationName the GraphQL operation name
     * @param variables     the GraphQL variables
     * @param httpRequest   the HTTP request
     * @return the GraphQL response
     */
    @Get(produces = APPLICATION_JSON, single = true)
    public Publisher<MutableHttpResponse<String>> get(
            @QueryValue("query") String query,
            @Nullable @QueryValue("operationName") String operationName,
            @Nullable @QueryValue("variables") String variables,
            HttpRequest httpRequest) {

        // https://graphql.org/learn/serving-over-http/#get-request
        //
        // When receiving an HTTP GET request, the GraphQL query should be specified in the "query" query string.
        // For example, if we wanted to execute the following GraphQL query:
        //
        // {
        //   me {
        //     name
        //   }
        // }
        //
        // This request could be sent via an HTTP GET like so:
        //
        // http://myapi/graphql?query={me{name}}
        //
        // Query variables can be sent as a JSON-encoded string in an additional query parameter called "variables".
        // If the query contains several named operations,
        // an "operationName" query parameter can be used to control which one should be executed.

        return executeRequest(query, operationName, convertVariablesJson(variables), httpRequest);
    }

    /**
     * Handles GraphQL {@code POST} requests.
     *
     * @param query         the GraphQL query
     * @param operationName the GraphQL operation name
     * @param variables     the GraphQL variables
     * @param body          the GraphQL request body
     * @param httpRequest   the HTTP request
     * @return the GraphQL response
     */
    @Post(consumes = ALL, produces = APPLICATION_JSON, single = true)
    public Publisher<MutableHttpResponse<String>> post(
            @Nullable @QueryValue("query") String query,
            @Nullable @QueryValue("operationName") String operationName,
            @Nullable @QueryValue("variables") String variables,
            @Nullable @Body String body,
            HttpRequest httpRequest) {

        Optional<MediaType> opt = httpRequest.getContentType();
        MediaType contentType = opt.orElse(null);

        if (body == null) {
            body = "";
        }

        // https://graphql.org/learn/serving-over-http/#post-request
        //
        // A standard GraphQL POST request should use the application/json content type,
        // and include a JSON-encoded body of the following form:
        //
        // {
        //   "query": "...",
        //   "operationName": "...",
        //   "variables": { "myVariable": "someValue", ... }
        // }

        if (APPLICATION_JSON_TYPE.equals(contentType)) {
            if (body.trim().startsWith("[")) {
                try {
                    GraphQLRequestBody[] requests = graphQLJsonSerializer.deserialize(body, GraphQLRequestBody[].class);
                    return executeBatchRequests(requests, httpRequest);
                } catch (RuntimeException e) {
                    throw new HttpStatusException(BAD_REQUEST, "Invalid JSON in GraphQL request body");
                }
            }

            GraphQLRequestBody request;
            try {
                request = graphQLJsonSerializer.deserialize(body, GraphQLRequestBody.class);
            } catch (RuntimeException e) {
                throw new HttpStatusException(BAD_REQUEST, "Invalid JSON in GraphQL request body");
            }
            if (request.getQuery() == null) {
                request.setQuery("");
            }
            return executeRequest(request, httpRequest);
        }

        // In addition to the above, we recommend supporting two additional cases:
        //
        // * If the "query" query string parameter is present (as in the GET example above),
        //   it should be parsed and handled in the same way as the HTTP GET case.

        if (query != null) {
            return executeRequest(query, operationName, convertVariablesJson(variables), httpRequest);
        }

        // * If the "application/graphql" Content-Type header is present,
        //   treat the HTTP POST body contents as the GraphQL query string.

        if (APPLICATION_GRAPHQL_TYPE.equals(contentType)) {
            return executeRequest(body, null, null, httpRequest);
        }

        throw new HttpStatusException(UNPROCESSABLE_ENTITY, "Could not process GraphQL request");
    }

    private Map<String, Object> convertVariablesJson(String jsonMap) {
        if (jsonMap == null) {
            return Collections.emptyMap();
        }
        try {
            return graphQLJsonSerializer.deserialize(jsonMap, Map.class);
        } catch (RuntimeException e) {
            throw new HttpStatusException(BAD_REQUEST, "Invalid JSON in GraphQL variables");
        }
    }

    private Publisher<MutableHttpResponse<String>> executeBatchRequests(GraphQLRequestBody[] requests, HttpRequest httpRequest) {
        MutableHttpResponse<String> httpResponse = HttpResponse.status(HttpStatus.OK);
        List<GraphQLRequestBody> batchRequests = requests == null ? Collections.emptyList() : Arrays.asList(requests);
        return Flux.fromIterable(batchRequests)
                .concatMap(request -> Flux.from(executeGraphQLRequest(request, httpRequest, httpResponse)))
                .collectList()
                .map(graphQLResponseBodies -> httpResponse.body(graphQLJsonSerializer.serialize(graphQLResponseBodies)));
    }

    private Publisher<MutableHttpResponse<String>> executeRequest(GraphQLRequestBody request, HttpRequest httpRequest) {
        MutableHttpResponse<String> httpResponse = HttpResponse.status(HttpStatus.OK);
        return Mono.from(executeGraphQLRequest(request, httpRequest, httpResponse))
                .map(graphQLResponseBody -> httpResponse.body(graphQLJsonSerializer.serialize(graphQLResponseBody)));
    }

    /**
     * Executes the GraphQL request and returns the serialized {@link GraphQLResponseBody}.
     *
     * @param query         the GraphQL query
     * @param operationName the GraphQL operation name
     * @param variables     the GraphQL variables
     * @param httpRequest   the HTTP request
     * @return the serialized GraphQL response
     */
    private Publisher<MutableHttpResponse<String>> executeRequest(
            String query,
            String operationName,
            Map<String, Object> variables,
            HttpRequest httpRequest) {
        return executeRequest(newRequestBody(query, operationName, variables), httpRequest);
    }

    private Publisher<GraphQLResponseBody> executeGraphQLRequest(
            GraphQLRequestBody request,
            HttpRequest httpRequest,
            MutableHttpResponse<String> httpResponse) {
        GraphQLInvocationData invocationData = new GraphQLInvocationData(
                normalizeQuery(request.getQuery()),
                request.getOperationName(),
                request.getVariables());
        // create empty response entity first and pass it to GraphQLInvocation
        Publisher<ExecutionResult> executionResult = graphQLInvocation.invoke(invocationData, httpRequest, httpResponse);
        return graphQLExecutionResultHandler.handleExecutionResult(executionResult);
    }

    private GraphQLRequestBody newRequestBody(String query, String operationName, Map<String, Object> variables) {
        GraphQLRequestBody request = new GraphQLRequestBody();
        request.setQuery(query);
        request.setOperationName(operationName);
        request.setVariables(variables);
        return request;
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query;
    }
}
