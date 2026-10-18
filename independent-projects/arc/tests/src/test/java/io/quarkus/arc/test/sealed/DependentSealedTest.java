package io.quarkus.arc.test.sealed;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.Dependent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class DependentSealedTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyDependent.class);

    @Test
    public void test() {
        assertNotNull(Arc.container().select(MyDependent.class).get());
    }

    @Dependent
    static sealed class MyDependent permits MyDependentSubclass {
    }

    static final class MyDependentSubclass extends MyDependent {
    }
}
