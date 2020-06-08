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

import example.domain.ChatMessage;
import example.repository.ChatRepository;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;

/**
 * @author Gerard Klijs
 */
@Singleton
public class StreamDataFetcher implements DataFetcher<Publisher<ChatMessage>> {

    private ChatRepository chatRepository;

    public StreamDataFetcher(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Override
    public Publisher<ChatMessage> get(DataFetchingEnvironment env) {
        return chatRepository.stream();
    }
}
