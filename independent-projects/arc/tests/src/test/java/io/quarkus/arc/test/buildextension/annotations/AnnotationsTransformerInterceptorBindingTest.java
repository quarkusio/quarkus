package io.quarkus.arc.test.buildextension.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AnnotationsTransformerInterceptorBindingTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(IWantToBeIntercepted.class, Simple.class, SimpleInterceptor.class)
            .annotationsTransformers(new SimpleTransformer())
            .build();

    @Test
    public void testInterception() {
        IWantToBeIntercepted wantToBeIntercepted = Arc.container()
                .instance(IWantToBeIntercepted.class)
                .get();
        assertEquals(10, wantToBeIntercepted.size());
    }

    static class SimpleTransformer implements AnnotationsTransformer {

        @Override
        public boolean appliesTo(Kind kind) {
            return kind == Kind.METHOD;
        }

        @Override
        public void transform(TransformationContext context) {
            if (context.isMethod() && context.getTarget()
                    .asMethod()
                    .name()
                    .equals("size")) {
                context.transform().add(Simple.class).done();
            }
        }

    }

    @Dependent
    static class IWantToBeIntercepted {

        // => add @Simple here
        public int size() {
            return 0;
        }

    }

}
