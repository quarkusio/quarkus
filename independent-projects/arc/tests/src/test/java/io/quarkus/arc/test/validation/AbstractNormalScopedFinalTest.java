package io.quarkus.arc.test.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class AbstractNormalScopedFinalTest {

    @RegisterExtension
    public ArcTestContainer container = createTestContainer();

    protected abstract ArcTestContainer createTestContainer();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DeploymentException);
    }

    @ApplicationScoped
    static final class Unproxyable {

        void ping() {
        }

    }

    @ApplicationScoped
    static class FieldProducerWithFinalClass {

        @Produces
        @ApplicationScoped
        public FinalFoo foo = new FinalFoo();
    }

    @ApplicationScoped
    static class MethodProducerWithFinalClass {

        @Produces
        @ApplicationScoped
        public FinalFoo createFoo() {
            return new FinalFoo();
        }
    }

    @ApplicationScoped
    static class FieldProducerWithWrongConstructor {

        @Produces
        @ApplicationScoped
        public WrongConstructorFoo foo = new WrongConstructorFoo("foo");
    }

    @ApplicationScoped
    static class MethodProducerWithWrongConstructor {

        @Produces
        @ApplicationScoped
        public WrongConstructorFoo foo() {
            return new WrongConstructorFoo("foo");
        }
    }

    @Vetoed
    static final class FinalFoo {

    }

    @Vetoed
    static class WrongConstructorFoo {

        @SuppressWarnings("unused")
        private WrongConstructorFoo() {
            // private constructor
        }

        public WrongConstructorFoo(String f) {
            // public but not no-arg
        }
    }

}
