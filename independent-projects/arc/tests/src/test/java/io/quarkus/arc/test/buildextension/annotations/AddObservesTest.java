package io.quarkus.arc.test.buildextension.annotations;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AddObservesTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(IWantToObserve.class)
            .annotationsTransformers(new AnnotationsTransformer() {

                @Override
                public boolean appliesTo(Kind kind) {
                    return Kind.METHOD == kind;
                }

                @Override
                public void transform(TransformationContext transformationContext) {
                    MethodInfo method = transformationContext.getTarget().asMethod();
                    if (method.name().equals("observe")) {
                        transformationContext.transform()
                                .add(AnnotationInstance.create(DotName.createSimple(Observes.class.getName()),
                                        MethodParameterInfo.create(method, (short) 0), new AnnotationValue[] {}))
                                .done();
                    }
                }
            }).build();

    @Test
    public void testObserved() {
        IWantToObserve.OBSERVED.set(false);
        Arc.container().beanManager().getEvent().select(String.class).fire("ok");
        assertTrue(IWantToObserve.OBSERVED.get());
    }

    @Singleton
    static class IWantToObserve {

        static final AtomicBoolean OBSERVED = new AtomicBoolean();

        public void observe(String event) {
            OBSERVED.set(true);
        }

    }

}
