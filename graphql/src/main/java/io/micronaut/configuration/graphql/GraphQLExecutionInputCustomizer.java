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

import graphql.ExecutionInput;
import io.micronaut.http.HttpRequest;
import org.reactivestreams.Publisher;

/**
 * An interface for customizing the {@link ExecutionInput}.
 * A custom implementation can be provided to transform the execution input to e.g. set a context or root object.
 *
 * @author Marcel Overdijk
 * @since 1.0
 * @see graphql.ExecutionInput#transform(java.util.function.Consumer)
 */
public interface GraphQLExecutionInputCustomizer {

    /**
     * Customizes the GraphQL execution input.
     *
     * @param executionInput the execution input
     * @param httpRequest the HTTP request
     * @return the GraphQL context object
     */
    Publisher<ExecutionInput> customize(ExecutionInput executionInput, HttpRequest httpRequest);
}
