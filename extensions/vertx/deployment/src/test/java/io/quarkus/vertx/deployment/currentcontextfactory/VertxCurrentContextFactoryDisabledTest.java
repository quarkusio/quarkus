package io.quarkus.vertx.deployment.currentcontextfactory;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.runtime.VertxCurrentContextFactory;

public class VertxCurrentContextFactoryDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().overrideConfigKey("quarkus.vertx.customize-arc-context",
            "false");

    @Test
    public void testCustomizedFactoryNotUsed() {
        assertFalse(Arc.container().getCurrentContextFactory() instanceof VertxCurrentContextFactory);
    }

}
