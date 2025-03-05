package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusUnitTest;

public class MissingCheckerForInclusivePermsValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .assertException(t -> {
                Assertions.assertEquals(RuntimeException.class, t.getClass(), t.getMessage());
                Assertions.assertTrue(t.getMessage().contains("@PermissionsAllowed annotation placed on"));
                Assertions.assertTrue(
                        t.getMessage().contains("SecuredBean#securedBean' has inclusive relation between its permissions"));
                Assertions.assertTrue(t.getMessage().contains("you must also define"));
                Assertions.assertTrue(t.getMessage().contains("@PermissionChecker for 'checker:missing' permissions"));
            });

    @Test
    public void test() {
        Assertions.fail();
    }

    @Singleton
    public static class SecuredBean {

        @PermissionsAllowed(value = { "checker", "checker:missing" }, inclusive = true)
        public void securedBean() {
            // EMPTY
        }

        @PermissionChecker("checker")
        public boolean check(SecurityIdentity identity) {
            return false;
        }
    }
}
