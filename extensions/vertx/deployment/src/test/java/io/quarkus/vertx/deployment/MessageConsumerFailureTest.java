package io.quarkus.vertx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;

public class MessageConsumerFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(SimpleBean.class));

    @Inject
    SimpleBean simpleBean;

    @Inject
    Vertx vertx;

    @Inject
    EventBus eventBus;

    @Test
    public void testFailure() throws InterruptedException {
        verifyFailure("foo", "java.lang.IllegalStateException: Foo is dead", false);
        verifyFailure("foo-message", "java.lang.NullPointerException", false);
        verifyFailure("foo-completion-stage", "java.lang.NullPointerException: Something is null", false);
        verifyFailure("foo-completion-stage-failure", "boom", true);
        verifyFailure("foo-uni", "java.lang.NullPointerException: Something is null", false);
        verifyFailure("foo-uni-failure", "boom", true);

        verifyFailure("foo-blocking", "java.lang.IllegalStateException: Red is dead", false);
        verifyFailure("foo-message-blocking", "java.lang.NullPointerException", false);
        verifyFailure("foo-completion-stage-blocking", "java.lang.NullPointerException: Something is null", false);
        verifyFailure("foo-completion-stage-failure-blocking", "boom", true);
        verifyFailure("foo-uni-blocking", "java.lang.NullPointerException: Something is null", false);
        verifyFailure("foo-uni-failure-blocking", "boom", true);
    }

    @Test
    public void testFailureNoReplyHandler() throws InterruptedException {
        Handler<Throwable> oldHandler = vertx.exceptionHandler();
        try {
            BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
            vertx.exceptionHandler(new Handler<Throwable>() {
                @Override
                public void handle(Throwable event) {
                    try {
                        synchronizer.put(event);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });
            eventBus.send("foo", "bar");
            Object ret = synchronizer.poll(2, TimeUnit.SECONDS);
            assertTrue(ret instanceof IllegalStateException);
            assertEquals("Foo is dead", ((IllegalStateException) ret).getMessage());
        } finally {
            vertx.exceptionHandler(oldHandler);
        }
    }

    void verifyFailure(String address, String expectedMessage, boolean explicit) throws InterruptedException {
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.request(address, "hello", ar -> {
            try {
                if (ar.cause() != null) {
                    synchronizer.put(ar.cause());
                } else {
                    synchronizer.put(false);
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });
        Object ret = synchronizer.poll(2, TimeUnit.SECONDS);
        assertTrue(ret instanceof ReplyException);
        ReplyException replyException = (ReplyException) ret;
        if (!explicit) {
            assertEquals(ConsumeEvent.FAILURE_CODE, replyException.failureCode());
        } else {
            assertEquals(ConsumeEvent.EXPLICIT_FAILURE_CODE, replyException.failureCode());
        }
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

        @ConsumeEvent("foo-completion-stage-failure")
        CompletionStage<String> failedCompletionStage(String message) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new IOException("boom"));
            return future;
        }

        @ConsumeEvent(value = "foo-uni")
        Uni<String> failUni(String message) {
            throw new NullPointerException("Something is null");
        }

        @ConsumeEvent(value = "foo-uni-failure")
        Uni<String> failedUni(String message) {
            return Uni.createFrom().failure(new IOException("boom"));
        }

        @ConsumeEvent(value = "foo-blocking", blocking = true)
        String failBlocking(String message) {
            throw new IllegalStateException("Red is dead");
        }

        @ConsumeEvent(value = "foo-message-blocking", blocking = true)
        void failMessageBlocking(Message<String> message) {
            throw new NullPointerException();
        }

        @ConsumeEvent(value = "foo-completion-stage-blocking", blocking = true)
        CompletionStage<String> failCompletionStageBlocking(String message) {
            throw new NullPointerException("Something is null");
        }

        @ConsumeEvent(value = "foo-completion-stage-failure-blocking", blocking = true)
        CompletionStage<String> failedCompletionStageBlocking(String message) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new IOException("boom"));
            return future;
        }

        @ConsumeEvent(value = "foo-uni-blocking", blocking = true)
        Uni<String> failUniBlocking(String message) {
            throw new NullPointerException("Something is null");
        }

        @ConsumeEvent(value = "foo-uni-failure-blocking", blocking = true)
        Uni<String> failedUniBlocking(String message) {
            return Uni.createFrom().failure(new IOException("boom"));
        }
    }

}
