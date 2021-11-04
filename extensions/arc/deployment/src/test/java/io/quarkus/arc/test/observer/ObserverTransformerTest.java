package io.quarkus.arc.test.observer;

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
import java.util.Set;
import java.util.function.Consumer;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.inject.Inject;

import io.quarkus.arc.deployment.ObserverTransformerBuildItem;
import io.quarkus.arc.processor.ObserverTransformer;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class ObserverTransformerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyObserver.class, AlphaQualifier.class, BravoQualifier.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new ObserverTransformerBuildItem(new ObserverTransformer() {

                            @Override
                            public boolean appliesTo(Type observedType, Set<AnnotationInstance> qualifiers) {
                                return observedType.name().equals(DotName.createSimple(MyEvent.class.getName()));
                            }

                            @Override
                            public void transform(TransformationContext context) {
                                if (context.getMethod().name().equals("")) {
                                    context.transform().removeAll().done();
                                }
                            }

                        }));
                    }
                }).produces(ObserverTransformerBuildItem.class).build();
            }
        };
    }

    @BravoQualifier
    @Inject
    Event<MyEvent> event;

    @Test
    public void testTransformation() {
        MyEvent myEvent = new MyEvent();
        event.fire(myEvent);
        // MyObserver.onMyEventRemoveQualifiers() would not match without transformation 
        assertEquals(1, myEvent.log.size());
        assertEquals("onMyEventRemoveQualifiers", myEvent.log.get(0));
    }

    @Singleton
    static class MyObserver {

        void onMyEventRemoveQualifiers(@Observes @BravoQualifier MyEvent event) {
            event.log.add("onMyEventRemoveQualifiers");
        }

    }

    @Qualifier
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface AlphaQualifier {

    }

    @Qualifier
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface BravoQualifier {

    }

    static class MyEvent {

        final List<String> log = new ArrayList<>();

    }

}
