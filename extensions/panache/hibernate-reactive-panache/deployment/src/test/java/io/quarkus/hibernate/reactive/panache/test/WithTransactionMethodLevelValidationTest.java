package io.quarkus.hibernate.reactive.panache.test;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.QuarkusUnitTest;

public class WithTransactionMethodLevelValidationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().setExpectedException(IllegalStateException.class)
            .withApplicationRoot(root -> root.addClasses(Bean.class));

    @Test
    public void testValidationFailed() {
        fail();
    }

    @Unremovable
    @ApplicationScoped
    static class Bean {

        @WithTransaction
        void ping() {
        }

    }
}
