/*
 * Copyright 2017-2026 original authors
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
/**
 * Development mode support for GraphQL. It is outside the package of the GraphQL integration, whose requirements apply
 * to its subpackages too, so that it exists, and asks for a restart, while GraphQL is disabled or no {@code GraphQL}
 * bean is defined yet.
 *
 * @author graemerocher
 * @since 5.2.0
 */
@NullMarked
package io.micronaut.graphql.dev;

import org.jspecify.annotations.NullMarked;
