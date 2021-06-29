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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;

import java.io.IOException;

/**
 * The Jackson implementation for serializing and deserializing GraphQL objects.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
@Singleton
public class JacksonGraphQLJsonSerializer implements GraphQLJsonSerializer {

    private final ObjectMapper objectMapper;

    /**
     * Default constructor.
     *
     * @param objectMapper the {@link ObjectMapper} instance
     */
    public JacksonGraphQLJsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException("Error serializing object to JSON: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T deserialize(String json, Class<T> requiredType) {
        try {
            return objectMapper.readValue(json, requiredType);
        } catch (IOException e) {
            throw new RuntimeException("Error deserializing object from JSON: " + e.getMessage(), e);
        }
    }
}
