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

package io.micronaut.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import org.reactivestreams.Publisher;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static io.micronaut.http.MediaType.APPLICATION_JSON;

/**
 * The GraphQL controller.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
@Controller("${graphql.url:/graphql}")
@Requires(property = "graphql.enabled", value = "true", defaultValue = "true")
@Requires(beans = GraphQL.class)
public class GraphQLController {

    protected static final String APPLICATION_JSON_UTF8 = APPLICATION_JSON + ";charset=UTF-8";

    private final GraphQLInvocation graphQLInvocation;
    private final ExecutionResultHandler executionResultHandler;
    private final ObjectMapper objectMapper;

    public GraphQLController(GraphQLInvocation graphQLInvocation, ExecutionResultHandler executionResultHandler,
            ObjectMapper objectMapper) {
        this.graphQLInvocation = graphQLInvocation;
        this.executionResultHandler = executionResultHandler;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles the GraphQL {@code GET} requests.
     *
     * @param query         the GraphQL query
     * @param operationName the GraphQL operation name
     * @param variables     the GraphQL variables
     * @param httpRequest   the HTTP request
     * @return the GraphQL response
     */
    @Get(produces = APPLICATION_JSON_UTF8)
    public Publisher<HttpResponse<GraphQLResponseBody>> get(
            @QueryValue("query") String query,
            @Nullable @QueryValue("operationName") String operationName,
            @Nullable @QueryValue("variables") String variables,
            HttpRequest httpRequest) {
        return executeRequest(query, operationName, convertVariablesJson(variables), httpRequest);
    }

    /**
     * Handles the GraphQL {@code POST} requests.
     *
     * @param body        the GraphQL request body
     * @param httpRequest the HTTP request
     * @return the GraphQL response
     */
    @Post(consumes = APPLICATION_JSON, produces = APPLICATION_JSON_UTF8)
    public Publisher<HttpResponse<GraphQLResponseBody>> post(@Body GraphQLRequestBody body, HttpRequest httpRequest) {
        String query = body.getQuery();
        if (query == null) {
            query = "";
        }
        return executeRequest(query, body.getOperationName(), body.getVariables(), httpRequest);
    }

    private Map<String, Object> convertVariablesJson(String jsonMap) {
        if (jsonMap == null) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(jsonMap, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not convert variables GET parameter: expected a JSON map", e);
        }
    }

    private Publisher<HttpResponse<GraphQLResponseBody>> executeRequest(
            String query,
            String operationName,
            Map<String, Object> variables,
            HttpRequest httpRequest) {
        GraphQLInvocationData invocationData = new GraphQLInvocationData(query, operationName, variables);
        Publisher<ExecutionResult> executionResult = graphQLInvocation.invoke(invocationData, httpRequest);
        return executionResultHandler.handleExecutionResult(executionResult);
    }
}
