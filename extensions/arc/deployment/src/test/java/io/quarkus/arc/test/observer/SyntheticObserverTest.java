package io.quarkus.arc.test.observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.test.QuarkusUnitTest;

public class SyntheticObserverTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyObserver.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        ObserverRegistrationPhaseBuildItem observerRegistrationPhase = context
                                .consume(ObserverRegistrationPhaseBuildItem.class);
                        context.produce(new ObserverConfiguratorBuildItem(
                                observerRegistrationPhase.getContext().configure()
                                        .beanClass(DotName.createSimple(SyntheticObserverTest.class.getName()))
                                        .observedType(String.class).notify(mc -> {
                                            ResultHandle events = mc
                                                    .readStaticField(
                                                            FieldDescriptor.of(MyObserver.class, "EVENTS", List.class));
                                            mc.invokeInterfaceMethod(
                                                    MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class),
                                                    events, mc.load("synthetic"));
                                            mc.returnValue(null);
                                        })));
                    }
                }).consumes(ObserverRegistrationPhaseBuildItem.class).produces(ObserverConfiguratorBuildItem.class).build();
            }
        };
    }

    @Test
    public void testSyntheticObserver() {
        MyObserver.EVENTS.clear();
        Arc.container().beanManager().fireEvent("foo");
        assertEquals(2, MyObserver.EVENTS.size(), "Events: " + MyObserver.EVENTS);
        assertTrue(MyObserver.EVENTS.contains("synthetic"));
        assertTrue(MyObserver.EVENTS.contains("foo_MyObserver"));
    }

    @Singleton
    static class MyObserver {

        public static final List<String> EVENTS = new CopyOnWriteArrayList<String>();

        void test(@Observes String event) {
            EVENTS.add(event + "_MyObserver");
        }

    }

}
