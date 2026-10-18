package io.quarkus.arc.test.buildextension.observers;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.ObserverTransformer;
import io.quarkus.arc.test.ArcTestContainer;

public class ObservertransformerTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(MyObserver.class, AlphaQualifier.class)
            .observerTransformers(new ObserverTransformer() {

                @Override
                public void transform(TransformationContext context) {
                    context.transform()
                            .reception(Reception.IF_EXISTS)
                            .priority(1)
                            .done();
                }

                @Override
                public boolean appliesTo(Type observedType, Set<AnnotationInstance> qualifiers) {
                    // observed type is String and qualifiers: @AlphaQualifier
                    return observedType.name().toString().equals(String.class.getName()) && qualifiers.size() == 1
                            && qualifiers.iterator().next().name().toString().equals(AlphaQualifier.class.getName());
                }
            }).build();

    @Test
    public void testTransformedObserver() {
        MyObserver.EVENTS.clear();

        @SuppressWarnings("serial")
        Event<String> event = Arc.container().beanManager().getEvent().select(String.class,
                new AlphaQualifier.Literal());
        event.fire("foo");
        // Reception was transformed to IF_EXISTS so test1() is not invoked
        assertEquals(List.of("foo_MyObserver2"), MyObserver.EVENTS);
        MyObserver.EVENTS.clear();

        event.fire("foo");
        assertEquals(List.of("foo_MyObserver1", "foo_MyObserver2"), MyObserver.EVENTS);
    }

    @Singleton
    static class MyObserver {

        static final List<String> EVENTS = new CopyOnWriteArrayList<String>();

        // Transformed to test1(@Observes(reception = IF_EXISTS) @Priority(1) @AlphaQualifier String event)
        void test1(@Observes @AlphaQualifier String event) {
            EVENTS.add(event + "_MyObserver1");
        }

        void test2(@Observes @Priority(10) String event) {
            EVENTS.add(event + "_MyObserver2");
        }

    }

    @Qualifier
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface AlphaQualifier {
        class Literal extends AnnotationLiteral<AlphaQualifier> implements AlphaQualifier {
        }
    }

}
