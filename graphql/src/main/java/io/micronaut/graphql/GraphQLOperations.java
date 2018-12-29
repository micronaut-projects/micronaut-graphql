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

import io.micronaut.core.async.annotation.SingleResult;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import org.reactivestreams.Publisher;

import javax.annotation.Nullable;

import static io.micronaut.http.MediaType.APPLICATION_JSON;

/**
 * The GraphQL operations.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
public interface GraphQLOperations {

    String APPLICATION_JSON_UTF8 = APPLICATION_JSON + ";charset=UTF-8";

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
    Publisher<GraphQLResponseBody> get(
            @QueryValue("query") String query,
            @Nullable @QueryValue("operationName") String operationName,
            @Nullable @QueryValue("variables") String variables);

    /**
     * Handles GraphQL {@code POST} requests.
     *
     * @param body the GraphQL request body
     * @return the GraphQL response
     */
    @Post(consumes = APPLICATION_JSON, produces = APPLICATION_JSON_UTF8, single = true)
    @SingleResult
    Publisher<GraphQLResponseBody> post(@Body GraphQLRequestBody body);
}
