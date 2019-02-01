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

import java.util.Map;

/**
 * Represents the request body input data to execute a GraphQL query.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
public class GraphQLRequestBody {

    private String query;
    private String operationName;
    private Map<String, Object> variables;

    /**
     * Returns the query.
     *
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the query.
     *
     * @param query the query
     */
    public void setQuery(final String query) {
        this.query = query;
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
     * Sets the operation name.
     *
     * @param operationName the operation name
     */
    public void setOperationName(final String operationName) {
        this.operationName = operationName;
    }

    /**
     * Returns the variables.
     *
     * @return the variables
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * Sets the variables.
     *
     * @param variables the variables
     */
    public void setVariables(final Map<String, Object> variables) {
        this.variables = variables;
    }
}
