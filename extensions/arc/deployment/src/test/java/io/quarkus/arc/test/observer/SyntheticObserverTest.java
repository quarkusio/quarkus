package io.quarkus.arc.test.observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
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
                                        .observedType(String.class)
                                        .notify(ng -> {
                                            BlockCreator bc = ng.notifyMethod();

                                            bc.withList(Expr.staticField(FieldDesc.of(MyObserver.class, "EVENTS")))
                                                    .add(Const.of("synthetic"));
                                            bc.return_();
                                        })));
                    }
                }).consumes(ObserverRegistrationPhaseBuildItem.class).produces(ObserverConfiguratorBuildItem.class).build();
            }
        };
    }

    @Test
    public void testSyntheticObserver() {
        MyObserver.EVENTS.clear();
        Arc.container().beanManager().getEvent().fire("foo");
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
