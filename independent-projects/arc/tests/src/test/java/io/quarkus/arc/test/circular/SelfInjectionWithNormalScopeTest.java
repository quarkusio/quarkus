package io.quarkus.arc.test.circular;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SelfInjectionWithNormalScopeTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(
            AbstractServiceImpl.class,
            ActualServiceImpl.class,
            Foo.class);

    @Test
    public void testDependencies() {
        Foo foo = CDI.current().select(Foo.class).get();
        assertNotNull(foo);
        assertEquals("pong", foo.ping());
    }

    static abstract class AbstractServiceImpl {
        @Inject
        protected Foo foo;
    }

    @ApplicationScoped
    static class ActualServiceImpl extends AbstractServiceImpl implements Foo {

        @Override
        public String ping() {
            return "pong";
        }
    }

    interface Foo {
        String ping();
    }
}
