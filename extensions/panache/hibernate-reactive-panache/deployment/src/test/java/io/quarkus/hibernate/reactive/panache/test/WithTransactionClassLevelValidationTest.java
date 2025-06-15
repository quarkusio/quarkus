package io.quarkus.hibernate.reactive.panache.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.QuarkusUnitTest;

public class WithTransactionClassLevelValidationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot(root -> root.addClasses(Bean.class));

    @Inject
    Bean bean;

    @Test
    public void testBindingIgnored() {
        assertEquals("ok", bean.ping());
    }

    @WithTransaction
    @Unremovable
    @ApplicationScoped
    static class Bean {

        // this method is just ignored by the interceptor
        String ping() {
            return "ok";
        }

    }
}
