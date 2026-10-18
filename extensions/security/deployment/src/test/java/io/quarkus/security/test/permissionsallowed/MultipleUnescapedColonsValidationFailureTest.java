package io.quarkus.security.test.permissionsallowed;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.test.QuarkusExtensionTest;

class MultipleUnescapedColonsValidationFailureTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .assertException(t -> {
                Assertions.assertEquals(RuntimeException.class, t.getClass(), t.getMessage());
                Assertions.assertTrue(t.getMessage().contains("system:role:query1"),
                        "Error should reference the invalid value: " + t.getMessage());
                Assertions.assertTrue(t.getMessage().contains("more than one unescaped colon"),
                        "Error should mention multiple unescaped colons: " + t.getMessage());
            });

    @Test
    void test() {
        Assertions.fail("Build was expected to fail due to multiple unescaped colons");
    }

    @Singleton
    public static class SecuredBean {

        @PermissionsAllowed("system:role:query1")
        public void securedMethod() {
        }
    }
}
