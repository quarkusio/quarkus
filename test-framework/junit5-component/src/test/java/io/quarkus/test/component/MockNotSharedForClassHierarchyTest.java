package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;

public class MockNotSharedForClassHierarchyTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = QuarkusComponentTestExtension.builder()
            .ignoreNestedClasses()
            .build();

    @Inject
    Component component;

    @InjectMock
    Alpha alpha;

    @Test
    public void testMock() {
        Mockito.when(alpha.ping()).thenReturn(42);
        Mockito.when(component.baz.ping()).thenReturn(1);
        assertEquals(42, component.alpha.ping());
        assertEquals(0, component.bar.ping());
        assertEquals(0, component.foo.ping());
        assertEquals(1, component.baz.ping());
        assertTrue(component.alpha != component.bar);
        assertTrue(component.bar != component.baz);
        assertTrue(component.baz != component.foo);
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
