package io.quarkus.arc.test.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.test.supplement.ConsumerOfSomeBeanInExternalLibrary;
import io.quarkus.arc.test.supplement.ConsumerOfSomeDepBeanInExternalLibrary;
import io.quarkus.arc.test.supplement.SomeBeanInExternalLibrary;
import io.quarkus.arc.test.supplement.SomeDepBeanInExternalLibrary;
import io.quarkus.arc.test.supplement.SomeEventInExternalLibrary;
import io.quarkus.arc.test.supplement.SomeInterfaceInExternalLibrary;
import io.quarkus.arc.test.supplement.SomeProducedDependencyInExternalLibrary;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

public class DecoratorOfExternalBeanTest {
    // the test includes an _application_ decorator (in the Runtime CL) that applies
    // to a bean that is _outside_ of the application (in the Base Runtime CL)

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyDecorator.class))
            // we need a non-application archive, so cannot use `withAdditionalDependency()`
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-arc-test-supplement", Version.getVersion())));

    @Inject
    SomeBeanInExternalLibrary bean;

    @Inject
    ConsumerOfSomeBeanInExternalLibrary consumer;

    @Inject
    SomeDepBeanInExternalLibrary depBean;

    @Inject
    ConsumerOfSomeDepBeanInExternalLibrary depConsumer;

    @Inject
    Event<SomeEventInExternalLibrary> event;

    @Inject
    Instance<SomeProducedDependencyInExternalLibrary> instance;

    @Test
    public void testNormalScope() {
        assertEquals("Delegated: Hello", bean.hello());

        assertFalse(SomeBeanInExternalLibrary.pinged);
        assertFalse(SomeBeanInExternalLibrary.observed);
        assertFalse(SomeBeanInExternalLibrary.produced);
        assertFalse(SomeBeanInExternalLibrary.disposed);

        assertEquals("pong", consumer.ping());

        assertTrue(SomeBeanInExternalLibrary.pinged);
        assertFalse(SomeBeanInExternalLibrary.observed);
        assertFalse(SomeBeanInExternalLibrary.produced);
        assertFalse(SomeBeanInExternalLibrary.disposed);

        event.fire(new SomeEventInExternalLibrary());

        assertTrue(SomeBeanInExternalLibrary.pinged);
        assertTrue(SomeBeanInExternalLibrary.observed);
        assertFalse(SomeBeanInExternalLibrary.produced);
        assertFalse(SomeBeanInExternalLibrary.disposed);

        SomeProducedDependencyInExternalLibrary dependency = instance.get();

        assertTrue(SomeBeanInExternalLibrary.pinged);
        assertTrue(SomeBeanInExternalLibrary.observed);
        assertTrue(SomeBeanInExternalLibrary.produced);
        assertFalse(SomeBeanInExternalLibrary.disposed);

        assertEquals("Produced: Hello", dependency.hello());

        assertTrue(SomeBeanInExternalLibrary.pinged);
        assertTrue(SomeBeanInExternalLibrary.observed);
        assertTrue(SomeBeanInExternalLibrary.produced);
        assertFalse(SomeBeanInExternalLibrary.disposed);

        instance.destroy(dependency);

        assertTrue(SomeBeanInExternalLibrary.pinged);
        assertTrue(SomeBeanInExternalLibrary.observed);
        assertTrue(SomeBeanInExternalLibrary.produced);
        assertTrue(SomeBeanInExternalLibrary.disposed);
    }

    @Test
    public void testPseudoScope() {
        assertEquals("Delegated: DepHello", depBean.hello());

        assertFalse(SomeDepBeanInExternalLibrary.pinged);

        assertEquals("pong", depConsumer.ping());

        assertTrue(SomeDepBeanInExternalLibrary.pinged);
    }

    @Test
    public void testNonAppArchive() {
        assertTrue(SomeInterfaceInExternalLibrary.class.getClassLoader().getName()
                .contains("Quarkus Base Runtime ClassLoader"));
        assertTrue(SomeBeanInExternalLibrary.class.getClassLoader().getName()
                .contains("Quarkus Base Runtime ClassLoader"));
        assertTrue(ConsumerOfSomeBeanInExternalLibrary.class.getClassLoader().getName()
                .contains("Quarkus Base Runtime ClassLoader"));
        assertTrue(SomeDepBeanInExternalLibrary.class.getClassLoader().getName()
                .contains("Quarkus Base Runtime ClassLoader"));
        assertTrue(ConsumerOfSomeDepBeanInExternalLibrary.class.getClassLoader().getName()
                .contains("Quarkus Base Runtime ClassLoader"));
        assertTrue(SomeEventInExternalLibrary.class.getClassLoader().getName()
                .contains("Quarkus Base Runtime ClassLoader"));
        assertTrue(SomeProducedDependencyInExternalLibrary.class.getClassLoader().getName()
                .contains("Quarkus Base Runtime ClassLoader"));

        // client proxies are non-app
        assertTrue(bean.getClass().getSimpleName().endsWith("_ClientProxy"));
        assertTrue(bean.getClass().getClassLoader().getName().contains("Quarkus Base Runtime ClassLoader"));
        assertTrue(consumer.getClass().getSimpleName().endsWith("_ClientProxy"));
        assertTrue(consumer.getClass().getClassLoader().getName().contains("Quarkus Base Runtime ClassLoader"));
    }

    @Test
    public void testAppArchive() {
        assertTrue(MyDecorator.class.getClassLoader().getName().contains("Quarkus Runtime ClassLoader"));

        // decoration subclasses are app
        SomeBeanInExternalLibrary unwrappedBean = ClientProxy.unwrap(bean);
        assertTrue(unwrappedBean.getClass().getSimpleName().endsWith("_Subclass"));
        assertTrue(unwrappedBean.getClass().getClassLoader().getName().contains("Quarkus Runtime ClassLoader"));
        assertTrue(depBean.getClass().getSimpleName().endsWith("_Subclass"));
        assertTrue(depBean.getClass().getClassLoader().getName().contains("Quarkus Runtime ClassLoader"));
    }

    @Decorator
    public static class MyDecorator implements SomeInterfaceInExternalLibrary {
        @Inject
        @Delegate
        SomeInterfaceInExternalLibrary delegate;

        @Override
        public String hello() {
            return "Delegated: " + delegate.hello();
        }
    }
}
