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

import io.micronaut.http.HttpRequest;

/**
 * The GraphQL root builder.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
public interface GraphQLRootBuilder {

    /**
     * Builds the GraphQL root object to start the query execution on.
     *
     * @param httpRequest the HTTP request
     * @return the root object
     */
    Object build(HttpRequest httpRequest);
}
