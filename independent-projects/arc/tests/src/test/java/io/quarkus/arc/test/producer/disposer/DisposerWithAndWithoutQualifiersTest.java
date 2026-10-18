package io.quarkus.arc.test.producer.disposer;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class DisposerWithAndWithoutQualifiersTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(Producer.class, Foo.class, Bar.class);

    @Test
    public void testDisposers() {
        InstanceHandle<String> handle = Arc.container().instance(String.class);
        assertEquals("default", handle.get());
        assertEquals(0, Producer.destroyed.size());
        handle.destroy();
        assertEquals(1, Producer.destroyed.size());
        assertEquals("default", Producer.destroyed.get(0));

        handle = Arc.container().instance(String.class, new Foo.Literal());
        assertEquals("foo", handle.get());
        assertEquals(1, Producer.destroyed.size());
        handle.destroy();
        assertEquals(2, Producer.destroyed.size());
        assertEquals("foo", Producer.destroyed.get(1));
    }

    @Singleton
    static class Producer {
        static final List<String> destroyed = new ArrayList<>();

        @Produces
        @Dependent
        String produce() {
            return "default";
        }

        @Produces
        @Dependent
        @Foo
        String produceFoo() {
            return "foo";
        }

        void dispose(@Disposes String str) {
            destroyed.add(str);
        }

        void disposeFoo(@Disposes @Foo String str) {
            destroyed.add(str);
        }

        // this one should be ignored
        void disposeBar(@Disposes @Bar String str) {
            destroyed.add(str);
        }
    }

    @Qualifier
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface Foo {
        class Literal extends AnnotationLiteral<Foo> implements Foo {
        }
    }

    @Qualifier
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface Bar {
    }
}
