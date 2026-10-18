package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CachedResults;
import io.quarkus.test.QuarkusExtensionTest;

public class CachedResultsInjectFailureTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(MujBean.class))
            .setExpectedException(IllegalStateException.class);

    @Inject
    @CachedResults
    MujBean mujBean;

    @Test
    public void testFailure() {
        fail();
    }

    @Dependent
    public static class MujBean {

        public MujBean(BeanManager beanManager) {
        }

    }

}
