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
package example.repository;

import example.domain.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Alexey Zhokhov
 */
@Singleton
public class AuthorRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorRepository.class);

    private final Map<String, Author> authors = new HashMap<>();

    public List<Author> findAllById(Collection<String> ids) {
        LOG.debug("Batch loading authors: {}", ids);

        return authors.values()
                .stream()
                .filter(it -> ids.contains(it.getId()))
                .collect(Collectors.toList());
    }

    public Author findOrCreate(String username) {
        if (!authors.containsKey(username)) {
            authors.put(username, new Author(UUID.randomUUID().toString(), username));
        }

        return authors.get(username);
    }

}
