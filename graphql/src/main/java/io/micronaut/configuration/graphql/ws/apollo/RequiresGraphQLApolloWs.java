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
package io.micronaut.configuration.graphql.ws.apollo;

import graphql.GraphQL;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta annotation for GraphQL web socket requirements.
 *
 * @author Gerard Klijs
 * @since 1.3
 * @deprecated The Apollo subscriptions-transport-ws protocol is deprecated and its usage should be replaced with the new graphql-ws implementation.
 */
@Deprecated(since = "4.0")
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Requires(property = GraphQLApolloWsConfiguration.ENABLED_CONFIG, notEquals = StringUtils.FALSE)
@Requires(beans = GraphQL.class)
public @interface RequiresGraphQLApolloWs {
}
