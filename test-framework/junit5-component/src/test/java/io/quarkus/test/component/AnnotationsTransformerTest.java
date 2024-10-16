package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.beans.Charlie;

public class AnnotationsTransformerTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = QuarkusComponentTestExtension.builder()
            .addAnnotationsTransformer(AnnotationsTransformer.appliedToClass()
                    .whenClass(c -> c.declaredAnnotations().isEmpty()
                            && c.annotationsMap().containsKey(DotName.createSimple(Inject.class)))
                    .thenTransform(t -> t.add(Singleton.class)))
            .build();

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

}
