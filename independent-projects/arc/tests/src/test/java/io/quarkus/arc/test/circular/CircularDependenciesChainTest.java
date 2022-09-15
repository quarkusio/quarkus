package io.quarkus.arc.test.circular;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.util.Comparator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CircularDependenciesChainTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(
            Foo.class,
            Bar.class,
            Baz.class, Producing.class);

    @Test
    public void testDependencies() {
        Foo foo = Arc.container().instance(Foo.class).get();
        assertNotNull(foo);
        assertEquals("foo is not null", foo.ping());
        assertEquals(0, Arc.container().instance(Producing.class).get().getComparator().compare("A", "A"));
    }

    @ApplicationScoped
    static class Foo {
        @Inject
        Bar bar;

        String ping() {
            return bar.ping();
        }
    }

    @ApplicationScoped
    static class Bar {
        @Inject
        Baz baz;

        String ping() {
            return baz.ping();
        }
    }

    @ApplicationScoped
    static class Baz {
        @Inject
        Foo foo;

        String ping() {
            return foo == null ? "foo is null" : "foo is not null";
        }
    }

    @ApplicationScoped
    static class Producing {

        private Comparator<String> comparator;

        @ApplicationScoped
        @Produces
        Comparator<String> producedComparator = Comparator.naturalOrder();

        @Inject
        public void setComparator(Comparator<String> comparator) {
            this.comparator = comparator;
        }

        public Comparator<String> getComparator() {
            return comparator;
        }

    }

}
