package io.quarkus.test.component.unsatisfied;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class UnsatisfiedDependencyClassHierarchyTest {

    @Inject
    Consumer consumer;

    @InjectMock
    Foo fooMock;

    @InjectMock
    Bar barMock;

    @Test
    public void testAutoMocks() {
        Mockito.when(fooMock.val()).thenReturn("fooMock");
        Mockito.when(barMock.val()).thenReturn("barMock");
        assertEquals("fooMock", consumer.foo.val());
        assertEquals("barMock", consumer.bar.val());
    }

    @Singleton
    public static class Consumer {

        @Inject
        Foo foo; // -> register mock synthetic bean for bean types [Foo] and default qualifiers

        @Inject
        Bar bar; // -> register mock synthetic bean for bean types [Bar] and default qualifiers

    }

    public static class Foo {

        String val() {
            return "foo";
        }

    }

    public static class Bar extends Foo {

        @Override
        String val() {
            return super.val() + "bar";
        }

    }

}
