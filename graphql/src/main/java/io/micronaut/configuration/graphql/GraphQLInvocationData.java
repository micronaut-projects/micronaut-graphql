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

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Map;

/**
 * Represents the data to execute a GraphQL query.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
@Immutable
public class GraphQLInvocationData {

    private final String query;
    private final String operationName;
    private final Map<String, Object> variables;

    /**
     * Default constructor.
     *
     * @param query         the query
     * @param operationName the operation name
     * @param variables     the variables
     */
    public GraphQLInvocationData(String query, @Nullable String operationName, @Nullable Map<String, Object> variables) {
        this.query = query;
        this.operationName = operationName;
        this.variables = variables != null ? variables : Collections.emptyMap();
    }

    /**
     * Returns the query.
     *
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Returns the operation name.
     *
     * @return the operation name
     */
    public String getOperationName() {
        return operationName;
    }

    /**
     * Returns the variables.
     *
     * @return the variables
     */
    public Map<String, Object> getVariables() {
        return variables;
    }
}
