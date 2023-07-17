package io.quarkus.arc.test.instance;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.TestLiteral;

public class InvalidQualifierInstanceTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Alpha.class);

    @SuppressWarnings("serial")
    @Test
    public void testIllegalArgumentException() {
        assertThatThrownBy(() -> Arc.container().instance(Alpha.class).get().instance.select(new TestLiteral()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Singleton
    static class Alpha {

        @Inject
        Instance<Object> instance;

    }

}
