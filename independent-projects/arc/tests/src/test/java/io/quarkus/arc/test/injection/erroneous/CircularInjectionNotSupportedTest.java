package io.quarkus.arc.test.injection.erroneous;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.arc.test.ArcTestContainer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CircularInjectionNotSupportedTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder().beanClasses(Foo.class, AbstractServiceImpl.class,
            ActualServiceImpl.class).shouldFail().build();

    @Test
    public void testExceptionThrown() {
        Throwable error = container.getFailure();
        assertThat(error).isInstanceOf(IllegalStateException.class);
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
