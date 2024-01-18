package io.quarkus.arc.test.context.optimized;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ServiceLoader;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ComponentsProvider;
import io.quarkus.test.QuarkusUnitTest;

public class OptimizeContextsDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(SimpleBean.class))
            .overrideConfigKey("quarkus.arc.optimize-contexts", "false");

    @Inject
    SimpleBean bean;

    @Test
    public void testContexts() {
        assertTrue(bean.ping());
        for (ComponentsProvider componentsProvider : ServiceLoader.load(ComponentsProvider.class)) {
            assertTrue(componentsProvider.getComponents().getContextInstances().isEmpty());
        }
    }

}
