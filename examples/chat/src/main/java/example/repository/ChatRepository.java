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

package example.repository;

import example.domain.ChatMessage;
import io.reactivex.*;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author Gerard Klijs
 */
@Singleton
public class ChatRepository {

    private List<ChatMessage> chats = new ArrayList<>();
    private BlockingQueue<ChatMessage> blockingQueue = new ArrayBlockingQueue<>(10);
    private Flowable<ChatMessage> stream = Flowable.create((FlowableOnSubscribe<ChatMessage>) emitter -> {
        while (!emitter.isCancelled()) {
            emitter.onNext(blockingQueue.take());
        }
        emitter.onComplete();
    }, BackpressureStrategy.BUFFER).share();

    public Iterable<ChatMessage> findAll() {
        return chats;
    }

    public Iterable<ChatMessage> findAfter(ZonedDateTime after) {
        return chats.stream()
                    .filter(chat -> chat.getTime().isAfter(after))
                    .collect(Collectors.toList());
    }

    public ChatMessage save(String text, String from) {
        ChatMessage chatMessage = new ChatMessage(text, from);
        chats.add(chatMessage);
        blockingQueue.add(chatMessage);
        return chatMessage;
    }

    public Publisher<ChatMessage> stream() {
        return stream;
    }
}
