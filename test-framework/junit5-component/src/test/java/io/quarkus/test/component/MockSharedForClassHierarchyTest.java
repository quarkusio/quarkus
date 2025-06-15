package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

public class MockSharedForClassHierarchyTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = QuarkusComponentTestExtension.builder().mock(Foo.class)
            .createMockitoMock(foo -> {
                Mockito.when(foo.ping()).thenReturn(11);
            }).ignoreNestedClasses().build();

    @Inject
    Component component;

    @Test
    public void testMock() {
        assertTrue(component.alpha == component.bar);
        assertTrue(component.bar == component.baz);
        assertTrue(component.baz == component.foo);

        assertEquals(11, component.alpha.ping());
        assertEquals(11, component.bar.ping());
        assertEquals(11, component.foo.ping());
        assertEquals(11, component.baz.ping());

        Mockito.when(component.baz.ping()).thenReturn(111);
        assertEquals(111, component.alpha.ping());
        assertEquals(111, component.bar.ping());
        assertEquals(111, component.foo.ping());
    }

    @Singleton
    static class Component {

        @Inject
        Alpha alpha;

        @Inject
        Bar bar;

        @Inject
        Foo foo;

        @Inject
        Baz baz;

    }

    @Singleton
    static class Foo extends Bar {

        @Override
        public int ping() {
            return 15;
        }

    }

    static class Bar extends Baz {

        @Override
        public int ping() {
            return 10;
        }

    }

    static class Baz implements Alpha {

    }

    interface Alpha {

        default int ping() {
            return 5;
        }

    }
}
