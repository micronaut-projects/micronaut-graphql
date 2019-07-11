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

package io.micronaut.graphql.annotations;

import io.micronaut.context.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Predefined {@link GraphQLDataFetcher} with a type name Mutation.</p>
 * <p>By default all public methods of the class are considered {@link Executable} and
 * are mapped as {@link graphql.schema.DataFetcher} with a method name as a default field name if not overridden.</p>
 *
 * @author Denis Stepanov
 * @since 1.3
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE})
@GraphQLDataFetcher(typeName = "Mutation")
public @interface GraphQLMutationDataFetcher {

    /**
     * @return The field name of a GraphQL object type
     */
    @AliasFor(annotation = GraphQLFieldName.class, member = "value")
    String fieldName() default "";

}
