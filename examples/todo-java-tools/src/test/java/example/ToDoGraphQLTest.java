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
package example;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class ToDoGraphQLTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    public void queryToDosResolvesNestedAuthorInNativeCompatiblePath() {
        Map<String, Object> result = execute("{ toDos { id title completed author { id username } } }");

        Map<String, Object> data = data(result);
        List<Map<String, Object>> toDos = (List<Map<String, Object>>) data.get("toDos");
        assertNotNull(toDos);
        assertFalse(toDos.isEmpty());

        Map<String, Object> firstToDo = toDos.getFirst();
        assertNotNull(firstToDo.get("id"));
        assertNotNull(firstToDo.get("title"));
        assertNotNull(firstToDo.get("completed"));

        Map<String, Object> author = (Map<String, Object>) firstToDo.get("author");
        assertNotNull(author);
        assertNotNull(author.get("id"));
        assertNotNull(author.get("username"));
    }

    @Test
    public void mutationCreateToDoResolvesNestedAuthorInNativeCompatiblePath() {
        Map<String, Object> result = execute(
            "mutation { createToDo(title:\"demo-pr\", author:\"alice\") { id title completed author { id username } } }"
        );

        Map<String, Object> data = data(result);
        Map<String, Object> toDo = (Map<String, Object>) data.get("createToDo");
        assertNotNull(toDo);
        assertEquals("demo-pr", toDo.get("title"));
        assertEquals(Boolean.FALSE, toDo.get("completed"));

        Map<String, Object> author = (Map<String, Object>) toDo.get("author");
        assertNotNull(author);
        assertEquals("alice", author.get("username"));
        assertNotNull(author.get("id"));
    }

    private Map<String, Object> execute(String query) {
        Map<String, Object> result = client.toBlocking().retrieve(
            HttpRequest.POST("/graphql", Map.of("query", query)),
            Map.class
        );
        assertNotNull(result);
        assertFalse(result.containsKey("errors"));
        return result;
    }

    private Map<String, Object> data(Map<String, Object> result) {
        assertTrue(result.containsKey("data"));
        return (Map<String, Object>) result.get("data");
    }
}
