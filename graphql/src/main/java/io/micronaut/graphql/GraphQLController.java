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
import io.micronaut.core.async.annotation.SingleResult;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.context.ServerRequestContext;
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
@Controller("${" + GraphQLConfiguration.PATH + ":" + GraphQLConfiguration.DEFAULT_PATH + "}")
@Requires(property = GraphQLConfiguration.ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
@Requires(beans = GraphQL.class)
public class GraphQLController {

    private static final String APPLICATION_JSON_UTF8 = APPLICATION_JSON + ";charset=UTF-8";

    private final GraphQLInvocation graphQLInvocation;
    private final GraphQLExecutionResultHandler graphQLExecutionResultHandler;
    private final ObjectMapper objectMapper;

    /**
     * Default constructor.
     *
     * @param graphQLInvocation             the {@link GraphQLInvocation} instance
     * @param graphQLExecutionResultHandler the {@link GraphQLExecutionResultHandler} instance
     * @param objectMapper                  the {@link ObjectMapper} instance
     */
    public GraphQLController(GraphQLInvocation graphQLInvocation, GraphQLExecutionResultHandler graphQLExecutionResultHandler,
            ObjectMapper objectMapper) {
        this.graphQLInvocation = graphQLInvocation;
        this.graphQLExecutionResultHandler = graphQLExecutionResultHandler;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles GraphQL {@code GET} requests.
     *
     * @param query         the GraphQL query
     * @param operationName the GraphQL operation name
     * @param variables     the GraphQL variables
     * @return the GraphQL response
     */
    @Get(produces = APPLICATION_JSON_UTF8, single = true)
    @SingleResult
    public Publisher<GraphQLResponseBody> get(
            @QueryValue("query") String query,
            @Nullable @QueryValue("operationName") String operationName,
            @Nullable @QueryValue("variables") String variables) {
        return executeRequest(query, operationName, convertVariablesJson(variables));
    }

    /**
     * Handles GraphQL {@code POST} requests.
     *
     * @param body the GraphQL request body
     * @return the GraphQL response
     */
    @Post(consumes = APPLICATION_JSON, produces = APPLICATION_JSON_UTF8, single = true)
    @SingleResult
    public Publisher<GraphQLResponseBody> post(@Body GraphQLRequestBody body) {
        String query = body.getQuery();
        if (query == null) {
            query = "";
        }
        return executeRequest(query, body.getOperationName(), body.getVariables()
        );
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

    private Publisher<GraphQLResponseBody> executeRequest(String query, String operationName, Map<String, Object> variables) {
        HttpRequest httpRequest = ServerRequestContext.currentRequest().orElse(null);
        GraphQLInvocationData invocationData = new GraphQLInvocationData(query, operationName, variables);
        Publisher<ExecutionResult> executionResult = graphQLInvocation.invoke(invocationData, httpRequest);
        return graphQLExecutionResultHandler.handleExecutionResult(executionResult);
    }
}
