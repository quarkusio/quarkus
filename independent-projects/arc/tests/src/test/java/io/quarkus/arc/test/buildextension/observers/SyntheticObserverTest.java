package io.quarkus.arc.test.buildextension.observers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.ObserverRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

public class SyntheticObserverTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(MyObserver.class, Named.class)
            .observerRegistrars(new ObserverRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure().observedType(String.class).notify(og -> {
                        BlockCreator bc = og.notifyMethod();

                        Expr event = bc.invokeInterface(MethodDesc.of(EventContext.class, "getEvent", Object.class),
                                og.eventContext());
                        bc.withList(Expr.staticField(FieldDesc.of(MyObserver.class, "EVENTS"))).add(event);
                        bc.return_();
                    }).done();

                    context.configure().observedType(String.class)
                            .addQualifier().annotation(Named.class).addValue("value", "bla").done()
                            .notify(og -> {
                                BlockCreator bc = og.notifyMethod();

                                bc.withList(Expr.staticField(FieldDesc.of(MyObserver.class, "EVENTS")))
                                        .add(Const.of("synthetic2"));
                                bc.return_();
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
