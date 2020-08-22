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
import graphql.ExecutionResult;

/**
 * Holder class for GraphQL execution input of graphql query execution and the result of performing a graphql query.
 *
 * @author Alexey Zhokhov
 * @since 1.4.0
 */
public class GraphQLExecution {

    private final ExecutionInput input;
    private final ExecutionResult result;

    public GraphQLExecution(ExecutionInput input, ExecutionResult result) {
        this.input = input;
        this.result = result;
    }

    public ExecutionInput getInput() {
        return input;
    }

    public ExecutionResult getResult() {
        return result;
    }

}
