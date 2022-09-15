package io.quarkus.arc.test.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AbstractClassIgnoredTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Foo.class, Bar.class);

    @Test
    public void testBeans() {
        assertEquals(1, Arc.container().beanManager().getBeans(Foo.class).size());
        assertEquals("bar", Arc.container().instance(Foo.class).get().ping());

    }

    @ApplicationScoped
    static abstract class Foo {

        protected String ping() {
            return "foo";
        }

    }

    @ApplicationScoped
    static class Bar extends Foo {

        @Override
        protected String ping() {
            return "bar";
        }

    }
}
