/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Context;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class MessageConsumerMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(SimpleBean.class));

    @Inject
    SimpleBean simpleBean;

    @Test
    public void testSend() throws InterruptedException {
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.send("foo", "hello", ar -> {
            if (ar.succeeded()) {
                try {
                    synchronizer.put(ar.result().body());
                } catch (InterruptedException e) {
                    fail(e);
                }
            } else {
                fail(ar.cause());
            }
        });
        assertEquals("HELLO", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testSendAsync() throws InterruptedException {
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.send("foo-async", "hello", ar -> {
            if (ar.succeeded()) {
                try {
                    synchronizer.put(ar.result().body());
                } catch (InterruptedException e) {
                    fail(e);
                }
            } else {
                fail(ar.cause());
            }
        });
        assertEquals("olleh", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testSendDefaultAddress() throws InterruptedException {
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.send("io.quarkus.vertx.MessageConsumerMethodTest$SimpleBean", "Hello", ar -> {
            if (ar.succeeded()) {
                try {
                    synchronizer.put(ar.result().body());
                } catch (InterruptedException e) {
                    fail(e);
                }
            } else {
                fail(ar.cause());
            }
        });
        assertEquals("hello", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testPublish() throws InterruptedException {
        SimpleBean.MESSAGES.clear();
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
        SimpleBean.latch = new CountDownLatch(2);
        eventBus.publish("pub", "Hello");
        SimpleBean.latch.await(2, TimeUnit.SECONDS);
        assertTrue(SimpleBean.MESSAGES.contains("hello"));
        assertTrue(SimpleBean.MESSAGES.contains("HELLO"));
    }

    @Test
    public void testBlockingConsumer() throws InterruptedException {
        SimpleBean.MESSAGES.clear();
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
        SimpleBean.latch = new CountDownLatch(1);
        eventBus.publish("blocking", "Hello");
        SimpleBean.latch.await(2, TimeUnit.SECONDS);
        assertEquals(1, SimpleBean.MESSAGES.size());
        String message = SimpleBean.MESSAGES.get(0);
        assertTrue(message.contains("hello::true"));
        System.out.println(message);
    }

    static class SimpleBean {

        static volatile CountDownLatch latch;

        static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

        @ConsumeEvent // io.quarkus.vertx.MessageConsumerMethodTest$SimpleBean
        String sendDefaultAddress(String message) {
            return message.toLowerCase();
        }

        @ConsumeEvent("foo")
        String reply(String message) {
            return message.toUpperCase();
        }

        @ConsumeEvent("pub")
        void consume(String message) {
            MESSAGES.add(message.toLowerCase());
            latch.countDown();
        }

        @ConsumeEvent("pub")
        void consume(Message<String> message) {
            MESSAGES.add(message.body().toUpperCase());
            latch.countDown();
        }

        @ConsumeEvent("foo-async")
        CompletionStage<String> replyAsync(String message) {
            return CompletableFuture.completedFuture(new StringBuilder(message).reverse().toString());
        }

        @ConsumeEvent(value = "blocking", blocking = true)
        void consumeBlocking(String message) {
            MESSAGES.add(message.toLowerCase() + "::" + Context.isOnWorkerThread());
            latch.countDown();
        }
    }

}
