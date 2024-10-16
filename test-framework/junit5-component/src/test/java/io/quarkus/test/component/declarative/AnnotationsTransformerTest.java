package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.beans.Charlie;
import io.quarkus.test.component.declarative.AnnotationsTransformerTest.MyAnnotationsTransformer;

@QuarkusComponentTest(annotationsTransformers = MyAnnotationsTransformer.class)
public class AnnotationsTransformerTest {

    @Inject
    NotABean bean;

    @InjectMock
    Charlie charlie;

    @Test
    public void testPing() {
        Mockito.when(charlie.ping()).thenReturn("foo");
        assertEquals("foo", bean.ping());
    }

    // @Singleton should be added automatically
    public static class NotABean {

        @Inject
        Charlie charlie;

        public String ping() {
            return charlie.ping();
        }

    }

    public static class MyAnnotationsTransformer implements AnnotationsTransformer {

        @Override
        public boolean appliesTo(Kind kind) {
            return Kind.CLASS == kind;
        }

        @Override
        public void transform(TransformationContext context) {
            ClassInfo c = context.getTarget().asClass();
            if (c.declaredAnnotations().isEmpty()
                    && c.annotationsMap().containsKey(DotName.createSimple(Inject.class))) {
                context.transform().add(Singleton.class).done();
            }
        }

    }

}
