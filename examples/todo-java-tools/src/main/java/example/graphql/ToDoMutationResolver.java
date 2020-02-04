/*
 * Copyright 2017-2019 original authors
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

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import example.domain.ToDo;
import example.repository.ToDoRepository;

import javax.inject.Singleton;

/**
 * @author Marcel Overdijk
 */
@Singleton
@SuppressWarnings("Duplicates")
public class ToDoMutationResolver implements GraphQLMutationResolver {

    private ToDoRepository toDoRepository;

    public ToDoMutationResolver(ToDoRepository toDoRepository) {
        this.toDoRepository = toDoRepository;
    }

    public ToDo createToDo(String title) {
        ToDo toDo = new ToDo(title);
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
