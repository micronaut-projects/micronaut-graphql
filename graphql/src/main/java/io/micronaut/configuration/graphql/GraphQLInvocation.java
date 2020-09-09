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

import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.ExecutionResult;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import org.reactivestreams.Publisher;

/**
 * An interface for invoking GraphQL request.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
public interface GraphQLInvocation {

    /**
     * Invokes the GraphQL request and returns a publisher that emits {@link GraphQLExecution} objects.
     *
     * @param invocationData the GraphQL invocation data
     * @param httpRequest    the HTTP request
     * @param httpResponse   the mutable HTTP response, can be {@literal null} when using websocket
     * @return the GraphQL execution result
     */
    Publisher<ExecutionResult> invoke(GraphQLInvocationData invocationData, HttpRequest httpRequest,
                                      @Nullable MutableHttpResponse<String> httpResponse);
}
