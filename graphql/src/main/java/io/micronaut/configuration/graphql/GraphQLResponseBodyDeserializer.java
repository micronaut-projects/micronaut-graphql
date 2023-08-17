/*
 * Copyright 2017-2023 original authors
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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.Decoder;
import io.micronaut.serde.Deserializer;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Map;

/**
 * Deserializes GraphQLResponseBody.
 */
@Singleton
public class GraphQLResponseBodyDeserializer implements Deserializer<GraphQLResponseBody> {

    @Override
    public @Nullable GraphQLResponseBody deserialize(
        @NonNull Decoder decoder,
        @NonNull DecoderContext context,
        @NonNull Argument<? super GraphQLResponseBody> type
    ) throws IOException {
        return new GraphQLResponseBody((Map<String, Object>) decoder.decodeArbitrary());
    }
}
