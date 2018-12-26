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

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import org.reactivestreams.Publisher;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The default GraphQL invocation.
 *
 * @author Marcel Overdijk
 * @author graemerocher
 * @since 1.0
 */
public class DefaultGraphQLInvocation implements GraphQLInvocation {

    private final GraphQL graphQL;
    private final GraphQLContextBuilder contextBuilder;

    /**
     * Default constructor.
     * @param graphQL The {@link GraphQL} instance
     * @param contextBuilder The {@link GraphQLContextBuilder} instance
     */
    public DefaultGraphQLInvocation(GraphQL graphQL, GraphQLContextBuilder contextBuilder) {
        this.graphQL = graphQL;
        this.contextBuilder = contextBuilder;
    }

    @Override
    public Publisher<ExecutionResult> invoke(GraphQLInvocationData invocationData, HttpRequest httpRequest) {
        return Publishers.fromCompletableFuture(() -> {
            Object context = contextBuilder != null ? contextBuilder.build(httpRequest) : null;
            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                    .context(context)
                    .query(invocationData.getQuery())
                    .operationName(invocationData.getOperationName())
                    .variables(invocationData.getVariables())
                    .build();
            return graphQL.executeAsync(executionInput);
        });
    }
}
