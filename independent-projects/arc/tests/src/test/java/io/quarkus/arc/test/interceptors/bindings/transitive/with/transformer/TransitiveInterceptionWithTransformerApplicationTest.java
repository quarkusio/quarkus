package io.quarkus.arc.test.interceptors.bindings.transitive.with.transformer;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.test.ArcTestContainer;
import org.jboss.jandex.AnnotationTarget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests transitive interceptor bindings when annotation transformers were applied
 */
public class TransitiveInterceptionWithTransformerApplicationTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(PlainBinding.class,
            PlainInterceptor.class, MuchCoolerBinding.class, MuchCoolerInterceptor.class, DummyBean.class)
            .annotationsTransformers(new TransitiveInterceptionWithTransformerApplicationTest.MyTransformer()).build();

    @Test
    public void testTransformersAreApplied() {
        Assertions.assertTrue(Arc.container().instance(DummyBean.class).isAvailable());
        DummyBean bean = Arc.container().instance(DummyBean.class).get();

        Assertions.assertTrue(PlainInterceptor.timesInvoked == 0);
        Assertions.assertTrue(MuchCoolerInterceptor.timesInvoked == 0);
        bean.ping();
        Assertions.assertTrue(PlainInterceptor.timesInvoked == 1);
        Assertions.assertTrue(MuchCoolerInterceptor.timesInvoked == 1);
    }

    static class MyTransformer implements AnnotationsTransformer {

        @Override
        public boolean appliesTo(AnnotationTarget.Kind kind) {
            return kind == AnnotationTarget.Kind.CLASS;
        }

        @Override
        public void transform(TransformationContext context) {
            if (context.isClass() && context.getTarget().asClass().name().toString().equals(PlainBinding.class.getName())) {
                // add MuchCoolerBinding
                context.transform().add(MuchCoolerBinding.class).done();
            }
        }

    }
}
