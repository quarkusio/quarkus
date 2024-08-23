package io.quarkus.vertx.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.Message;

public class NonVoidMessageConsumerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(MessageConsumers.class))
            .assertException(t -> {
                Throwable root = ExceptionUtil.getRootCause(t);
                assertTrue(
                        root.getMessage().contains(
                                "An event consumer business method that accepts io.vertx.core.eventbus.Message or io.vertx.mutiny.core.eventbus.Message must return void"),
                        t.toString());
            });

    @Test
    public void test() throws InterruptedException {
        fail();
    }

    @ApplicationScoped
    static class MessageConsumers {

        @ConsumeEvent("pub")
        String pub1(Message<String> name) {
            return "foo";
        }

    }

}
