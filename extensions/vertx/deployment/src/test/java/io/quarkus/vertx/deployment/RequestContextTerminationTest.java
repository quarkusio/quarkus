package io.quarkus.vertx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.EventBus;

public class RequestContextTerminationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(SimpleBean.class));

    @Inject
    EventBus eventBus;

    @Test
    public void testTermination() throws InterruptedException {
        assertTerminated("foo");
        assertTerminated("foo-cs");
        assertTerminated("foo-uni");
    }

    void assertTerminated(String address) throws InterruptedException {
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        Converter.DESTROYED.set(false);
        eventBus.request(address, "bongo", ar -> {
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
        assertEquals("BONGO", synchronizer.poll(2, TimeUnit.SECONDS));
        assertTrue(Converter.DESTROYED.get());
    }

    @Test
    public void testFailureNoReplyHandler() throws InterruptedException {
    }

    static class SimpleBean {

        @Inject
        Converter converter;

        @ConsumeEvent("foo")
        String foo(String message) {
            return converter.convert(message);
        }

        @ConsumeEvent("foo-cs")
        CompletionStage<String> asyncFoo(String message) {
            return CompletableFuture.completedFuture(converter.convert(message));
        }

        @ConsumeEvent("foo-uni")
        Uni<String> asyncFooUni(String message) {
            return Uni.createFrom().item(converter.convert(message));
        }

    }

    @RequestScoped
    static class Converter {

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        String convert(String val) {
            return val.toUpperCase();
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

}
