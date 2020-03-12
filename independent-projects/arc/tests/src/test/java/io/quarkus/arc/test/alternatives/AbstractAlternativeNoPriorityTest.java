package io.quarkus.arc.test.alternatives;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Vetoed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests that in Arc alternatives (class beans, producer methods/fields) without @Priority cause an exception.
 * Has three subclasses that separately test the same with bean class, producer field and producer method.
 */
public abstract class AbstractAlternativeNoPriorityTest {

    protected ArcTestContainer.Builder sharedBuilder = ArcTestContainer.builder().shouldFail().beanClasses(MyInterface.class,
            Foo.class);

    protected abstract ArcTestContainer buildContainer(ArcTestContainer.Builder sharedBuilder);

    @RegisterExtension
    ArcTestContainer container = buildContainer(sharedBuilder);

    @Test
    public void testExceptionWasThrown() {
        Throwable t = container.getFailure();
        assertNotNull(t);
        assertTrue(t instanceof IllegalStateException);
    }

    static interface MyInterface {
        void ping();
    }

    @ApplicationScoped
    static class Foo implements MyInterface {

        @Override
        public void ping() {

        }
    }

    @ApplicationScoped
    // deliberately doesn't have priority
    @Alternative
    static class AlternativeClassBean implements MyInterface {

        @Override
        public void ping() {

        }
    }

    // producers aren't really enabled, but it still simulates the error
    @Dependent
    static class ProducerWithField {
        @Produces
        @Alternative
        @ApplicationScoped
        AlternativeProducerFieldBean bar = new AlternativeProducerFieldBean();
    }

    // producers aren't really enabled, but it still simulates the error
    @Dependent
    static class ProducerWithMethod {

        @Produces
        @Alternative
        @ApplicationScoped
        public AlternativeProducerMethodBean createBar() {
            return new AlternativeProducerMethodBean();
        }
    }

    @Vetoed
    static class AlternativeProducerFieldBean implements MyInterface {

        @Override
        public void ping() {

        }
    }

    @Vetoed
    static class AlternativeProducerMethodBean implements MyInterface {

        @Override
        public void ping() {

        }
    }
}
