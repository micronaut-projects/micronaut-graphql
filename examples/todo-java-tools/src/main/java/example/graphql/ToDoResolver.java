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
package example.graphql;

import example.domain.Author;
import example.domain.ToDo;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;

import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

/**
 * @author Alexey Zhokhov
 */
@Singleton
public class ToDoResolver implements GraphQLResolver<ToDo> {

    @SuppressWarnings("unused")
    public CompletableFuture<Author> author(ToDo todo, DataFetchingEnvironment env) {
        DataLoader<String, Author> dataLoader = env.getDataLoader("author");

        return dataLoader.load(todo.getAuthorId());
    }

}
