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

import example.domain.ChatMessage;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * @author Gerard Klijs
 */
@Singleton
public class ChatRepository {

    private final List<ChatMessage> chats = new ArrayList<>();
    private final BlockingQueue<ChatMessage> blockingQueue = new ArrayBlockingQueue<>(10);
    private final Flux<ChatMessage> stream = Flux.create((Consumer<FluxSink<ChatMessage>>) sink -> {
        while (!sink.isCancelled()) {
            try {
                sink.next(blockingQueue.take());
            } catch (InterruptedException e) {
                sink.error(e);
            }
        }
        sink.complete();
    }, FluxSink.OverflowStrategy.BUFFER).share();

    public Iterable<ChatMessage> findAll() {
        return chats;
    }

    public Iterable<ChatMessage> findAfter(ZonedDateTime after) {
        return chats.stream()
            .filter(chat -> chat.getTime().isAfter(after))
            .toList();
    }

    public ChatMessage save(String text, String from) {
        var chatMessage = new ChatMessage(text, from);
        chats.add(chatMessage);
        blockingQueue.add(chatMessage);
        return chatMessage;
    }

    public Publisher<ChatMessage> stream() {
        return stream;
    }
}
