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

package io.micronaut.configuration.graphql.runtime;

import graphql.schema.idl.RuntimeWiring;

/**
 * Interface to allow different implementations to contribute to creation of {@link RuntimeWiring}
 * using {@link RuntimeWiring.Builder}.
 *
 * @author Denis Stepanov
 * @since 1.3
 */
public interface GraphQLRuntimeWiringBuilderCustomizer {

    /**
     * @param runtimeBuilder the builder to be used for type configuration
     */
    void accept(RuntimeWiring.Builder runtimeBuilder);

}
