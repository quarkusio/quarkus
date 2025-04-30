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

import jakarta.enterprise.inject.Any;
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

public class DisposerWithQualifiersTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(Producer.class, Dependency.class, Foo.class, Bar.class);

    @Test
    public void testDisposers() {
        InstanceHandle<String> handle = Arc.container().instance(String.class, new Foo.Literal());
        assertEquals("produced", handle.get());
        assertEquals(0, Producer.destroyed.size());
        handle.destroy();
        assertEquals(1, Producer.destroyed.size());
        assertEquals("produced", Producer.destroyed.get(0));
    }

    @Singleton
    static class Producer {
        static final List<String> destroyed = new ArrayList<>();

        @Singleton
        @Produces
        @Foo
        String produce(@Bar Dependency ignored) {
            return "produced";
        }

        void disposeFoo(@Disposes @Any String str) {
            destroyed.add(str);
        }

        // this one should be ignored
        void disposeBar(@Disposes @Bar String str) {
            destroyed.add(str);
        }

        // this one should be ignored too
        void disposeDefault(@Disposes String str) {
            destroyed.add(str);
        }
    }

    @Singleton
    @Bar
    static class Dependency {
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
