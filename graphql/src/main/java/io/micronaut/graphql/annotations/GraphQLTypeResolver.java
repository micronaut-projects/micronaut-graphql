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
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.DefaultScope;
import io.micronaut.context.annotation.Executable;

import javax.inject.Singleton;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Indicates that the role of a class is a GraphQL type resolver.</p>
 * <p>
 * <p>Class needs to implement {@link graphql.schema.TypeResolver}</p>
 *
 * @author Denis Stepanov
 * @since 1.3
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Executable
@Bean
@DefaultScope(Singleton.class)
@GraphQLTypeMapping
public @interface GraphQLTypeResolver {

    /**
     * @return The name of a GraphQL object type that TypeResolver should be mapped to
     */
    @AliasFor(annotation = GraphQLTypeName.class, member = "value")
    String typeName();

}
