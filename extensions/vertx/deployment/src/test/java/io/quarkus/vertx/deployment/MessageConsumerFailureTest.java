package io.quarkus.vertx.deployment;

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
import io.quarkus.vertx.ConsumeEvent;
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
        verifyFailure("foo", "java.lang.IllegalStateException: Foo is dead");
        verifyFailure("foo-message", "java.lang.NullPointerException");
        verifyFailure("foo-completion-stage", "java.lang.NullPointerException: Something is null");
    }

    void verifyFailure(String address, String expectedMessage) throws InterruptedException {
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
