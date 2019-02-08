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

package io.micronaut.configuration.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import org.reactivestreams.Publisher;

import javax.annotation.Nullable;
import javax.inject.Singleton;

/**
 * The default implementation for invoking GraphQL requests.
 *
 * @author Marcel Overdijk
 * @author Graeme Rocher
 * @since 1.0
 * @see GraphQL#executeAsync(ExecutionInput.Builder)
 */
@Singleton
public class DefaultGraphQLInvocation implements GraphQLInvocation {

    private final GraphQL graphQL;
    private final GraphQLContextBuilder graphQLContextBuilder;
    private final GraphQLRootBuilder graphQLRootBuilder;

    /**
     * Default constructor.
     *
     * @param graphQL               the {@link GraphQL} instance
     * @param graphQLContextBuilder the {@link GraphQLContextBuilder} instance
     * @param graphQLRootBuilder    the {@link GraphQLRootBuilder} instance
     */
    public DefaultGraphQLInvocation(GraphQL graphQL,
                                    @Nullable GraphQLContextBuilder graphQLContextBuilder,
                                    @Nullable GraphQLRootBuilder graphQLRootBuilder) {
        this.graphQL = graphQL;
        this.graphQLContextBuilder = graphQLContextBuilder;
        this.graphQLRootBuilder = graphQLRootBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Publisher<ExecutionResult> invoke(GraphQLInvocationData invocationData, HttpRequest httpRequest) {
        return Publishers.fromCompletableFuture(() -> {
            Object context = graphQLContextBuilder != null ? graphQLContextBuilder.build(httpRequest) : null;
            Object root = graphQLRootBuilder != null ? graphQLRootBuilder.build(httpRequest) : null;
            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                    .query(invocationData.getQuery())
                    .operationName(invocationData.getOperationName())
                    .context(context)
                    .root(root)
                    .variables(invocationData.getVariables())
                    .build();
            return graphQL.executeAsync(executionInput);
        });
    }
}
