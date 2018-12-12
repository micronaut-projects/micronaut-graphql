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

package io.micronaut.configuration.graphql;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * The GraphQL response.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
public class GraphQLResponseBody {

    private final Map<String, Object> specification;

    public GraphQLResponseBody(Map<String, Object> specification) {
        this.specification = specification;
    }

    @JsonAnyGetter
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public Map<String, Object> getSpecification() {
        return specification;
    }
}
