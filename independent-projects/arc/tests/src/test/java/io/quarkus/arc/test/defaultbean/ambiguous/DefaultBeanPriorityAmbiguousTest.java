package io.quarkus.arc.test.defaultbean.ambiguous;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.test.ArcTestContainer;

public class DefaultBeanPriorityAmbiguousTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyBean.class, FooInterface.class, FooImpl1.class, FooImpl2.class)
            .shouldFail()
            .build();

    @Test
    public void testAmbiguousResolution() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DeploymentException);
        assertTrue(error.getMessage().contains("AmbiguousResolutionException: Ambiguous dependencies"));
    }

    @Singleton
    static class MyBean {

        @Inject
        FooInterface foo;
    }

    interface FooInterface {
        String ping();
    }

    @ApplicationScoped
    @DefaultBean
    @Priority(10)
    static class FooImpl1 implements FooInterface {

        @Override
        public String ping() {
            return FooImpl1.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @DefaultBean
    @Priority(10)
    static class FooImpl2 implements FooInterface {

        @Override
        public String ping() {
            return FooImpl2.class.getSimpleName();
        }
    }
}
