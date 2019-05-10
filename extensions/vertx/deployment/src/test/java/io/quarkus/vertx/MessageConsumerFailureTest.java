/*
 * Copyright 2019 Red Hat, Inc.
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;

public class MessageConsumerFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(SimpleBean.class));

    @Inject
    SimpleBean simpleBean;

    @Inject
    EventBus eventBus;

    @Test
    public void testFailure() throws InterruptedException {
        verifyFailure("foo", "Foo is dead");
        verifyFailure("foo-message", null);
        verifyFailure("foo-completion-stage", "Something is null");
    }

    void verifyFailure(String address, String expectedMessage) throws InterruptedException {
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.send(address, "hello", ar -> {
            if (ar.cause() != null) {
                try {
                    synchronizer.put(ar.cause());
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        Object ret = synchronizer.poll(2, TimeUnit.SECONDS);
        assertTrue(ret instanceof ReplyException);
        ReplyException replyException = (ReplyException) ret;
        assertEquals(ConsumeEvent.FAILURE_CODE, replyException.failureCode());
        assertEquals(expectedMessage, replyException.getMessage());
    }

    static class SimpleBean {

        @ConsumeEvent("foo")
        String fail(String message) {
            throw new IllegalStateException("Foo is dead");
        }

        @ConsumeEvent("foo-message")
        void failMessage(Message<String> message) {
            throw new NullPointerException();
        }

        @ConsumeEvent("foo-completion-stage")
        CompletionStage<String> failCompletionStage(String message) {
            throw new NullPointerException("Something is null");
        }

    }

}
