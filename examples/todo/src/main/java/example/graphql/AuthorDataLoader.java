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
import example.repository.AuthorRepository;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.dataloader.MappedBatchLoader;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * @author Alexey Zhokhov
 */
@Singleton
@SuppressWarnings("Duplicates")
public class AuthorDataLoader implements MappedBatchLoader<String, Author> {

    private final AuthorRepository authorRepository;
    private final ExecutorService executor;

    public AuthorDataLoader(AuthorRepository authorRepository,
                            @Named(TaskExecutors.IO) ExecutorService executor) {
        this.authorRepository = authorRepository;
        this.executor = executor;
    }

    @Override
    public CompletionStage<Map<String, Author>> load(Set<String> keys) {
        return CompletableFuture.supplyAsync(() ->
                authorRepository
                        .findAllById(keys)
                        .stream()
                        .collect(toMap(Author::getId, Function.identity())), executor);
    }

}
