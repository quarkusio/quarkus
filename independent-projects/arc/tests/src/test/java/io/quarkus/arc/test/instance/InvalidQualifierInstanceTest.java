package io.quarkus.arc.test.instance;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
