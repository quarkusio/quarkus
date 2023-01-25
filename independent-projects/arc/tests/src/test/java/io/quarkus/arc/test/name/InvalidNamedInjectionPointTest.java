package io.quarkus.arc.test.name;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.DefinitionException;
import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class InvalidNamedInjectionPointTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Foo.class, Consumer.class)
            .shouldFail()
            .build();

    @Test
    public void test() {
        assertNotNull(container.getFailure());
        assertInstanceOf(DefinitionException.class, container.getFailure());
        assertTrue(container.getFailure().getMessage().contains("@Named without value may not be used on method parameter"));
    }

    @Named("foo")
    @Dependent
    static class Foo {
    }

    @Dependent
    static class Consumer {
        @Inject
        Consumer(@Named Foo foo) {
        }
    }
}
