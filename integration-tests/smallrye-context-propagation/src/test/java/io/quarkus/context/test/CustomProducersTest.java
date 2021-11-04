package io.quarkus.context.test;

import javax.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that user can override default beans for {@code ManagedExecutor} and {@code ThreadContext}.
 * Default beans are singletons (no proxy) whereas the newly defined beans here are application scoped.
 * Therefore it is enough to check that the injected values are proxied.
 */
public class CustomProducersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProducerBean.class));

    @Inject
    ManagedExecutor me;

    @Inject
    ThreadContext tc;

    @Test
    public void testDefaultBeansCanBeOverriden() {
        Assertions.assertNotNull(me);
        Assertions.assertNotNull(tc);
        Assertions.assertTrue(me instanceof ClientProxy);
        Assertions.assertTrue(tc instanceof ClientProxy);
    }
}
