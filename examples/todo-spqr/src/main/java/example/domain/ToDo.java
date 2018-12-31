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

package example.domain;

import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;

/**
 * @author Marcel Overdijk
 */
@SuppressWarnings("Duplicates")
public class ToDo {

    private String id;
    private String title;
    private boolean completed;

    public ToDo() {
    }

    public ToDo(String title) {
        this.title = title;
    }

    @GraphQLQuery(name = "id")
    public @GraphQLNonNull String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @GraphQLQuery(name = "title")
    public @GraphQLNonNull String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @GraphQLQuery(name = "completed")
    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
