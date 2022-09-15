package io.quarkus.arc.test.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ParameterizedBeanTypeWithVariableTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Foo.class);

    @Test
    public void testBeans() {
        assertEquals(1, Arc.container().beanManager().getBeans(Foo.class).size());
        assertEquals("foo", Arc.container().instance(Foo.class).get().ping());

    }

    @ApplicationScoped
    static class Foo<T> {

        protected String ping() {
            return "foo";
        }

    }
}
