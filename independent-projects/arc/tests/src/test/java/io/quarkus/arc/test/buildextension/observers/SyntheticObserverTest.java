package io.quarkus.arc.test.buildextension.observers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.ObserverRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SyntheticObserverTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(MyObserver.class, Named.class)
            .observerRegistrars(new ObserverRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure().observedType(String.class).notify(mc -> {
                        ResultHandle eventContext = mc.getMethodParam(0);
                        ResultHandle event = mc.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(EventContext.class, "getEvent", Object.class), eventContext);
                        ResultHandle events = mc.readStaticField(FieldDescriptor.of(MyObserver.class, "EVENTS", List.class));
                        mc.invokeInterfaceMethod(MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class),
                                events, event);
                        mc.returnValue(null);
                    }).done();

                    context.configure().observedType(String.class).addQualifier().annotation(Named.class)
                            .addValue("value", "bla").done()
                            .notify(mc -> {
                                ResultHandle events = mc
                                        .readStaticField(FieldDescriptor.of(MyObserver.class, "EVENTS", List.class));
                                mc.invokeInterfaceMethod(
                                        MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class),
                                        events, mc.load("synthetic2"));
                                mc.returnValue(null);
                            }).done();
                }
            }).build();

    @Test
    public void testSyntheticObserver() {
        MyObserver.EVENTS.clear();
        Arc.container().beanManager().getEvent().fire("foo");
        assertEquals(2, MyObserver.EVENTS.size(), "Events: " + MyObserver.EVENTS);
        assertTrue(MyObserver.EVENTS.contains("foo"));
        assertTrue(MyObserver.EVENTS.contains("foo_MyObserver"));

        MyObserver.EVENTS.clear();
        Arc.container().beanManager().getEvent().select(String.class, NamedLiteral.of("bla")).fire("foo");
        assertEquals(3, MyObserver.EVENTS.size(), "Events: " + MyObserver.EVENTS);
        assertTrue(MyObserver.EVENTS.contains("foo"));
        assertTrue(MyObserver.EVENTS.contains("foo_MyObserver"));
        assertTrue(MyObserver.EVENTS.contains("synthetic2"));
    }

    @Singleton
    static class MyObserver {

        public static final List<String> EVENTS = new CopyOnWriteArrayList<String>();

        void test(@Observes String event) {
            EVENTS.add(event + "_MyObserver");
        }

    }

}
