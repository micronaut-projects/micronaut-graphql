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
import graphql.ExecutionInput;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;

/**
 * The default implementation for customizing GraphQL execution inputs.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
@Singleton
@Requires(missingBeans = {GraphQLExecutionInputCustomizer.class})
public class DefaultGraphQLExecutionInputCustomizer implements GraphQLExecutionInputCustomizer {

    /**
     * {@inheritDoc}
     */
    @Override
    public Publisher<ExecutionInput> customize(ExecutionInput executionInput, HttpRequest httpRequest,
                                               @Nullable MutableHttpResponse<String> httpResponse) {
        return Publishers.just(executionInput);
    }
}
