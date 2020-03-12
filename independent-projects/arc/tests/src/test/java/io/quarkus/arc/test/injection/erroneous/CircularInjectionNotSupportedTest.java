package io.quarkus.arc.test.injection.erroneous;

import io.quarkus.arc.test.ArcTestContainer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.common.Assert;

public class CircularInjectionNotSupportedTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder().beanClasses(Foo.class, AbstractServiceImpl.class,
            ActualServiceImpl.class).shouldFail().build();

    @Test
    public void testExceptionThrown() {
        Throwable error = container.getFailure();
        Assertions.assertNotNull(error);
        Assert.assertTrue(error instanceof IllegalStateException);
    }

    static abstract class AbstractServiceImpl {
        @Inject
        protected Foo foo;
    }

    @Singleton
    static class ActualServiceImpl extends AbstractServiceImpl implements Foo {

        @Override
        public void ping() {
        }
    }

    interface Foo {
        void ping();
    }

}
