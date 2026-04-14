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
import io.micronaut.core.async.publisher.Publishers;
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
import io.micronaut.http.multipart.CompletedPart;
import io.micronaut.http.server.multipart.MultipartBody;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static io.micronaut.http.MediaType.ALL;
import static io.micronaut.http.MediaType.APPLICATION_GRAPHQL_TYPE;
import static io.micronaut.http.MediaType.APPLICATION_JSON;
import static io.micronaut.http.MediaType.APPLICATION_JSON_TYPE;
import static io.micronaut.http.MediaType.MULTIPART_FORM_DATA;

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
     * Handles GraphQL {@code POST} multipart requests.
     *
     * @param body        the multipart request body
     * @param httpRequest the HTTP request
     * @return the GraphQL response
     */
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Post(consumes = MULTIPART_FORM_DATA, produces = APPLICATION_JSON, single = true)
    public Publisher<MutableHttpResponse<String>> postMultipart(@Body MultipartBody body, HttpRequest httpRequest) {
        return Flux.from(body)
                .collectMap(CompletedPart::getName, part -> part)
                .flatMapMany(parts -> {
                    GraphQLRequestBody requestBody = graphQLJsonSerializer.deserialize(
                            partToString(getRequiredPart(parts, "operations")),
                            GraphQLRequestBody.class
                    );
                    if (requestBody.getQuery() == null) {
                        requestBody.setQuery("");
                    }
                    if (requestBody.getVariables() == null) {
                        requestBody.setVariables(new LinkedHashMap<>());
                    }
                    Map<String, Object> multipartMapping = convertVariablesJson(partToString(getRequiredPart(parts, "map")));
                    for (Map.Entry<String, Object> entry : multipartMapping.entrySet()) {
                        CompletedPart uploadedPart = getRequiredPart(parts, entry.getKey());
                        for (String variablePath : asVariablePaths(entry.getValue())) {
                            injectMultipartVariable(requestBody.getVariables(), variablePath, uploadedPart);
                        }
                    }
                    return Flux.from(executeRequest(
                            requestBody.getQuery(),
                            requestBody.getOperationName(),
                            requestBody.getVariables(),
                            httpRequest
                    ));
                });
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
            GraphQLRequestBody request;
            try {
                request = graphQLJsonSerializer.deserialize(body, GraphQLRequestBody.class);
            } catch (RuntimeException e) {
                throw new HttpStatusException(BAD_REQUEST, "Invalid JSON in GraphQL request body");
            }
            if (request.getQuery() == null) {
                request.setQuery("");
            }
            return executeRequest(request.getQuery(), request.getOperationName(), request.getVariables(), httpRequest);
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

    private CompletedPart getRequiredPart(Map<String, CompletedPart> parts, String name) {
        CompletedPart part = parts.get(name);
        if (part == null) {
            throw new HttpStatusException(UNPROCESSABLE_ENTITY, "Missing multipart field: " + name);
        }
        return part;
    }

    private List<String> asVariablePaths(Object mappingValue) {
        if (mappingValue instanceof String mappingPath) {
            return List.of(mappingPath);
        }
        if (mappingValue instanceof List<?> mappingPaths) {
            List<String> variablePaths = new ArrayList<>(mappingPaths.size());
            for (Object mappingPath : mappingPaths) {
                if (mappingPath instanceof String stringPath) {
                    variablePaths.add(stringPath);
                    continue;
                }
                throw new HttpStatusException(UNPROCESSABLE_ENTITY, "Invalid multipart mapping payload");
            }
            return variablePaths;
        }
        throw new HttpStatusException(UNPROCESSABLE_ENTITY, "Invalid multipart mapping payload");
    }

    @SuppressWarnings("unchecked")
    private void injectMultipartVariable(Map<String, Object> variables, String variablePath, CompletedPart part) {
        String[] pathSegments = variablePath.split("\\.");
        if (pathSegments.length < 2 || !"variables".equals(pathSegments[0])) {
            throw new HttpStatusException(UNPROCESSABLE_ENTITY, "Unsupported multipart variable path: " + variablePath);
        }

        Object current = variables;
        for (int i = 1; i < pathSegments.length - 1; i++) {
            String segment = pathSegments[i];
            if (current instanceof Map<?, ?> currentMap) {
                if (!currentMap.containsKey(segment)) {
                    throw new HttpStatusException(UNPROCESSABLE_ENTITY, "Unknown multipart variable path: " + variablePath);
                }
                current = ((Map<String, Object>) currentMap).get(segment);
                continue;
            }
            if (current instanceof List<?> currentList) {
                current = currentList.get(parsePathIndex(segment, currentList.size(), variablePath));
                continue;
            }
            throw new HttpStatusException(UNPROCESSABLE_ENTITY, "Unknown multipart variable path: " + variablePath);
        }

        String leafSegment = pathSegments[pathSegments.length - 1];
        if (current instanceof Map<?, ?> currentMap) {
            ((Map<String, Object>) currentMap).put(leafSegment, part);
            return;
        }
        if (current instanceof List<?> currentList) {
            ((List<Object>) currentList).set(parsePathIndex(leafSegment, currentList.size(), variablePath), part);
            return;
        }
        throw new HttpStatusException(UNPROCESSABLE_ENTITY, "Unknown multipart variable path: " + variablePath);
    }

    private int parsePathIndex(String segment, int size, String variablePath) {
        try {
            int index = Integer.parseInt(segment);
            if (index < 0 || index >= size) {
                throw new HttpStatusException(UNPROCESSABLE_ENTITY, "Unknown multipart variable path: " + variablePath);
            }
            return index;
        } catch (NumberFormatException e) {
            throw new HttpStatusException(UNPROCESSABLE_ENTITY, "Unknown multipart variable path: " + variablePath);
        }
    }

    private String partToString(CompletedPart part) {
        try {
            return new String(part.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new HttpStatusException(UNPROCESSABLE_ENTITY, "Could not process multipart field: " + part.getName());
        }
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
        GraphQLInvocationData invocationData = new GraphQLInvocationData(query, operationName, variables);
        // create empty response entity first and pass it to GraphQLInvocation
        MutableHttpResponse<String> httpResponse = HttpResponse.status(HttpStatus.OK);
        Publisher<ExecutionResult> executionResult = graphQLInvocation.invoke(invocationData, httpRequest, httpResponse);
        Publisher<GraphQLResponseBody> responseBody = graphQLExecutionResultHandler.handleExecutionResult(executionResult);
        return Publishers.map(responseBody, graphQLResponseBody -> httpResponse.body(graphQLJsonSerializer.serialize(graphQLResponseBody)));
    }
}
