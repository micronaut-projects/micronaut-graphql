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

import org.reactivestreams.Publisher;

/**
 * An interface for handling GraphQL {@link GraphQLExecution}s.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
public interface GraphQLExecutionResultHandler {

    /**
     * Handles the execution result by converting the provided execution result publisher to a publisher that emits
     * {@link GraphQLResponseBody} objects.
     *
     * @param executionResultPublisher the execution result
     * @return the response body
     */
    Publisher<GraphQLResponseBody> handleExecutionResult(Publisher<GraphQLExecution> executionResultPublisher);
}
