package io.quarkus.arc.test.instance;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class InvalidQualifierInstanceTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Alpha.class);

    @SuppressWarnings("serial")
    @Test
    public void testIllegalArgumentException() {
        assertThatThrownBy(() -> Arc.container().instance(Alpha.class).get().instance.select(new AnnotationLiteral<Test>() {
        }))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Singleton
    static class Alpha {

        @Inject
        Instance<Object> instance;

    }

}
