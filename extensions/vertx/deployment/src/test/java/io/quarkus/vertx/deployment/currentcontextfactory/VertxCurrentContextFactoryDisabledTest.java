package io.quarkus.vertx.deployment.currentcontextfactory;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.runtime.VertxCurrentContextFactory;

public class VertxCurrentContextFactoryDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.vertx.customize-arc-context", "false");

    @Test
    public void testCustomizedFactoryNotUsed() {
        assertFalse(Arc.container().getCurrentContextFactory() instanceof VertxCurrentContextFactory);
    }

}
