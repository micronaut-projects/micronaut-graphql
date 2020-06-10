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
package example.graphql

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import groovy.transform.CompileStatic

import javax.inject.Singleton

/**
 * @author Marcel Overdijk
 */
@Singleton
@CompileStatic
class HelloDataFetcher implements DataFetcher<String> {

    @Override
    String get(DataFetchingEnvironment env) {
        String name = env.getArgument("name")
        name = name?.trim() ?: "World"
        return "Hello ${name}!"
    }
}
