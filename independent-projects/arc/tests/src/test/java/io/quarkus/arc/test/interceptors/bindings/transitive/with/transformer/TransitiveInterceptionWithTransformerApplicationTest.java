package io.quarkus.arc.test.interceptors.bindings.transitive.with.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.jboss.jandex.AnnotationTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.test.ArcTestContainer;

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
        assertTrue(Arc.container().instance(DummyBean.class).isAvailable());
        DummyBean bean = Arc.container().instance(DummyBean.class).get();

        assertEquals(0, PlainInterceptor.timesInvoked);
        assertEquals(0, MuchCoolerInterceptor.timesInvoked);
        assertBindings(); // empty
        bean.ping();
        assertEquals(1, PlainInterceptor.timesInvoked);
        assertEquals(1, MuchCoolerInterceptor.timesInvoked);
        assertBindings(PlainBinding.class, MuchCoolerBinding.class);
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

    @SafeVarargs
    static void assertBindings(Class<? extends Annotation>... bindings) {
        assertBindings(PlainInterceptor.lastBindings, bindings);
        assertBindings(MuchCoolerInterceptor.lastBindings, bindings);
    }

    private static void assertBindings(Set<Annotation> actualBindings, Class<? extends Annotation>[] expectedBindings) {
        assertNotNull(actualBindings);
        assertEquals(expectedBindings.length, actualBindings.size());
        for (Class<? extends Annotation> expectedBinding : expectedBindings) {
            assertTrue(actualBindings.stream().anyMatch(it -> it.annotationType() == expectedBinding));
        }
    }
}
