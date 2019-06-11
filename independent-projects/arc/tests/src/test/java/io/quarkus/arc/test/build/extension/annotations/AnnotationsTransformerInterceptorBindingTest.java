package io.quarkus.arc.test.build.extension.annotations;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.Dependent;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.junit.Rule;
import org.junit.Test;

public class AnnotationsTransformerInterceptorBindingTest {

    @Rule
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
