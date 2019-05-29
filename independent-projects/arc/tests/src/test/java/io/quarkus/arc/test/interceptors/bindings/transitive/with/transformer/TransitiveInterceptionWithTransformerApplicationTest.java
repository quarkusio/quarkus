package io.quarkus.arc.test.interceptors.bindings.transitive.with.transformer;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.test.ArcTestContainer;
import org.jboss.jandex.AnnotationTarget;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests transitive interceptor bindings when annotation transformers were applied
 */
public class TransitiveInterceptionWithTransformerApplicationTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(PlainBinding.class,
            PlainInterceptor.class, MuchCoolerBinding.class, MuchCoolerInterceptor.class, DummyBean.class)
            .annotationsTransformers(new TransitiveInterceptionWithTransformerApplicationTest.MyTransformer()).build();

    @Test
    public void testTransformersAreApplied() {
        Assert.assertTrue(Arc.container().instance(DummyBean.class).isAvailable());
        DummyBean bean = Arc.container().instance(DummyBean.class).get();

        Assert.assertTrue(PlainInterceptor.timesInvoked == 0);
        Assert.assertTrue(MuchCoolerInterceptor.timesInvoked == 0);
        bean.ping();
        Assert.assertTrue(PlainInterceptor.timesInvoked == 1);
        Assert.assertTrue(MuchCoolerInterceptor.timesInvoked == 1);
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
