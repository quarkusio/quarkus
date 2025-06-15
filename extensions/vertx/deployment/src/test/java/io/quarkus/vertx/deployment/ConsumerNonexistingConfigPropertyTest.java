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

public class ConsumerNonexistingConfigPropertyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(MessageConsumers.class)).assertException(t -> {
                Throwable root = ExceptionUtil.getRootCause(t);
                assertTrue(
                        root.getMessage().contains(
                                "Could not expand value address.does.not.exist in property ${address.does.not.exist}"),
                        t.toString());
            });

    @Test
    public void test() throws InterruptedException {
        fail();
    }

    @ApplicationScoped
    static class MessageConsumers {

        @ConsumeEvent("${address.does.not.exist}")
        void pub(Message<String> name) {
        }

    }

}
