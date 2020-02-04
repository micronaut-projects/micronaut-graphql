/*
 * Copyright 2017-2019 original authors
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

/**
 * An interface for serializing and deserializing GraphQL objects.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
public interface GraphQLJsonSerializer {

    /**
     * Serializes the given object to a json {@link String}.
     *
     * @param object the object to serialize
     * @return the json string
     */
    String serialize(Object object);

    /**
     * Deserializes the given json {@link String} to an object of the required type.
     *
     * @param json         the json string
     * @param requiredType the required type
     * @param <T>          the required generic type
     * @return the object
     */
    <T> T deserialize(String json, Class<T> requiredType);
}
