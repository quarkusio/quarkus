package io.quarkus.test.component.unsatisfied;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;

import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class UnsatisfiedDependencyNonbindingQualifierMemberTest {

    @Inject
    FooConsumer consumer;

    @InjectMock
    @FooQualifier("ignored")
    Foo mock; // this mock is used for both FooConsumer#alpha nad FooConsumer#bravo

    @Test
    public void testAutoMocks() {
        Mockito.when(mock.val()).thenReturn("dummy");
        assertEquals("dummydummy", consumer.ping());
    }

    @Singleton
    public static class FooConsumer {

        @FooQualifier("alpha")
        Foo alpha;

        @FooQualifier("bravo")
        Foo bravo;

        public String ping() {
            return alpha.val() + bravo.val();
        }

    }

    public static class Foo {

        String val() {
            return "foo";
        }

    }

    @Qualifier
    @Retention(RUNTIME)
    public @interface FooQualifier {

        @Nonbinding
        String value();
    }

}
