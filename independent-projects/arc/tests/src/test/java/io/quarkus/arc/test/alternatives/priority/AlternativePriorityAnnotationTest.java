package io.quarkus.arc.test.alternatives.priority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class AlternativePriorityAnnotationTest {

    @RegisterExtension
    ArcTestContainer testContainer = new ArcTestContainer(MyInterface.class, Foo.class, AlternativeClassBean.class,
            MyProducer.class, AlternativeProducerFieldBean.class, AlternativeProducerMethodBean.class,
            ProducerWithClashingAnnotations.class, TheUltimateImpl.class);

    @Test
    public void testAnnotationWorks() {
        Instance<Object> instance = CDI.current().select(Object.class);
        assertTrue(instance.select(Foo.class).isResolvable());
        assertTrue(instance.select(AlternativeClassBean.class).isResolvable());
        assertTrue(instance.select(AlternativeProducerFieldBean.class).isResolvable());
        assertTrue(instance.select(AlternativeProducerMethodBean.class).isResolvable());
        assertTrue(instance.select(TheUltimateImpl.class).isResolvable());
        Instance<MyInterface> interfaceInstance = instance.select(MyInterface.class);
        assertTrue(interfaceInstance.isResolvable());
        MyInterface actualImpl = interfaceInstance.get();
        assertEquals(TheUltimateImpl.class.getSimpleName(), actualImpl.ping());
    }

    static interface MyInterface {
        String ping();
    }

    @ApplicationScoped
    static class Foo implements MyInterface {

        @Override
        public String ping() {
            return Foo.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @Alternative
    @Priority(2)
    static class AlternativeClassBean implements MyInterface {

        @Override
        public String ping() {
            return AlternativeClassBean.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @Alternative
    @Priority(1)
    static class ProducerWithClashingAnnotations {

        // Presence of double annotation denoting alternative shouldn't be a problem
        // this should be the selected alternative
        @Produces
        @ApplicationScoped
        @Alternative
        @Priority(1000)
        public TheUltimateImpl createUltimateImpl() {
            return new TheUltimateImpl();
        }
    }

    @Dependent
    @Alternative
    @Priority(200)
    static class MyProducer {
        @Produces
        @ApplicationScoped
        @Alternative
        @Priority(3)
        AlternativeProducerFieldBean bar = new AlternativeProducerFieldBean();

        @Produces
        @ApplicationScoped
        @Alternative
        @Priority(4)
        public AlternativeProducerMethodBean createBar() {
            return new AlternativeProducerMethodBean();
        }
    }

    @Vetoed
    static class AlternativeProducerFieldBean implements MyInterface {

        @Override
        public String ping() {
            return AlternativeProducerFieldBean.class.getSimpleName();
        }
    }

    @Vetoed
    static class AlternativeProducerMethodBean implements MyInterface {

        @Override
        public String ping() {
            return AlternativeProducerMethodBean.class.getSimpleName();
        }
    }

    @Vetoed
    static class TheUltimateImpl implements MyInterface {

        @Override
        public String ping() {
            return TheUltimateImpl.class.getSimpleName();
        }
    }
}
