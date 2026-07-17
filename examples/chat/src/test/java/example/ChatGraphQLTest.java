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
public class ChatGraphQLTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    public void queryMessages() {
        execute("mutation { chat(text: \"Hello world\") { from text time } }");

        Map<String, Object> result = execute("{ messages { from text time } }");

        Map<String, Object> data = data(result);
        List<Map<String, Object>> messages = (List<Map<String, Object>>) data.get("messages");
        assertNotNull(messages);
        assertFalse(messages.isEmpty());

        Map<String, Object> firstMessage = messages.getFirst();
        assertNotNull(firstMessage.get("from"));
        assertNotNull(firstMessage.get("text"));
        assertNotNull(firstMessage.get("time"));
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
