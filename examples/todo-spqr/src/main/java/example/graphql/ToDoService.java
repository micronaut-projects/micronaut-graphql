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

import example.domain.ToDo;
import example.repository.ToDoRepository;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import jakarta.inject.Singleton;

/**
 * @author Marcel Overdijk
 */
@Singleton
@SuppressWarnings("Duplicates")
public class ToDoService {

    private ToDoRepository toDoRepository;

    public ToDoService(ToDoRepository toDoRepository) {
        this.toDoRepository = toDoRepository;
    }

    @GraphQLQuery
    public @GraphQLNonNull Iterable<@GraphQLNonNull ToDo> toDos() {
        return toDoRepository.findAll();
    }

    @GraphQLMutation
    public ToDo createToDo(@GraphQLNonNull @GraphQLArgument(name = "title") String title) {
        ToDo toDo = new ToDo(title);
        return toDoRepository.save(toDo);
    }

    @GraphQLMutation
    public boolean completeToDo(@GraphQLNonNull @GraphQLArgument(name = "id") String id) {
        ToDo toDo = toDoRepository.findById(id);
        if (toDo != null) {
            toDo.setCompleted(true);
            toDoRepository.save(toDo);
            return true;
        } else {
            return false;
        }
    }

    @GraphQLMutation
    public boolean deleteToDo(@GraphQLNonNull @GraphQLArgument(name = "id") String id) {
        ToDo toDo = toDoRepository.findById(id);
        if (toDo != null) {
            toDoRepository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }
}
