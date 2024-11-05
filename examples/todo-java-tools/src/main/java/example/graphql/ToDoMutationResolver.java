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
import example.repository.AuthorRepository;
import example.repository.ToDoRepository;
import graphql.kickstart.tools.GraphQLMutationResolver;
import jakarta.inject.Singleton;

/**
 * @author Marcel Overdijk
 */
@Singleton
public class ToDoMutationResolver implements GraphQLMutationResolver {

    private final ToDoRepository toDoRepository;
    private final AuthorRepository authorRepository;

    public ToDoMutationResolver(ToDoRepository toDoRepository, AuthorRepository authorRepository) {
        this.toDoRepository = toDoRepository;
        this.authorRepository = authorRepository;
    }

    public ToDo createToDo(String title, String username) {
        Author author = authorRepository.findOrCreate(username);
        ToDo toDo = new ToDo(title, author.getId());
        return toDoRepository.save(toDo);
    }

    public Boolean completeToDo(String id) {
        ToDo toDo = toDoRepository.findById(id);
        if (toDo != null) {
            toDo.setCompleted(true);
            toDoRepository.save(toDo);
            return true;
        } else {
            return false;
        }
    }

    public Boolean deleteToDo(String id) {
        ToDo toDo = toDoRepository.findById(id);
        if (toDo != null) {
            toDoRepository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }
}
