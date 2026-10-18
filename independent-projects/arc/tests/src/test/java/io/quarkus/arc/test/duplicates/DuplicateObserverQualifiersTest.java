package io.quarkus.arc.test.duplicates;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Qualifier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.ObserverRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class DuplicateObserverQualifiersTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyQualifier.class)
            .observerRegistrars(new MyObserverRegistrar())
            .build();

    @Test
    public void test() {
        assertDoesNotThrow(() -> {
            Arc.container().select(new TypeLiteral<Event<MyEvent>>() {
            }).get().fire(new MyEvent());
        });
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyQualifier {
        String value();

        final class Literal extends AnnotationLiteral<MyQualifier> implements MyQualifier {
            private final String value;

            public Literal(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }
        }
    }

    static class MyEvent {
    }

    static class MyObserverRegistrar implements ObserverRegistrar {
        @Override
        public void register(RegistrationContext context) {
            Index index = null;
            try {
                index = Index.of(MyEvent.class, Object.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            context.configure()
                    .beanClass(DotName.createSimple(MyEvent.class))
                    .observedType(MyEvent.class)
                    .addQualifier(AnnotationInstance.builder(MyQualifier.class).value("foobar")
                            .buildWithTarget(index.getClassByName(Object.class)))
                    .addQualifier(AnnotationInstance.builder(MyQualifier.class).value("foobar")
                            .buildWithTarget(index.getClassByName(MyEvent.class)))
                    .notify(ng -> ng.notifyMethod().return_())
                    .done();
        }
    }
}
