package io.quarkus.arc.test.event.injection.illegal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class DisposerMethodEventRawTypeTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(DisposerMethodInjectionBean.class).shouldFail()
            .build();

    @Test
    public void testExceptionIsThrown() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("An injection point of raw type jakarta.enterprise.event.Event is defined"));
    }

    @Dependent
    public static class DisposerMethodInjectionBean {

        @Produces
        public Foo produceFoo() {
            return new Foo();
        }

        // rawtype Event
        public void disposeFoo(@Disposes Foo foo, Event event) {
        }

    }

    static class Foo {

    }
}
