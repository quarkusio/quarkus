package io.quarkus.security.test.permissionsallowed.checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusUnitTest;

public class PermissionCheckerActionsNotSupportedValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .assertException(t -> {
                var exceptionMessage = t.getMessage();
                assertEquals(IllegalArgumentException.class, t.getClass(), exceptionMessage);
                assertTrue(t.getMessage().contains("SecuredBean#check"), exceptionMessage);
                assertTrue(t.getMessage().contains("actions are currently not supported"), exceptionMessage);
            });

    @Test
    public void test() {
        Assertions.fail();
    }

    @Singleton
    public static class SecuredBean {

        @PermissionsAllowed("read:all")
        public void securedBean() {
            // EMPTY
        }

        @PermissionChecker("read:all")
        public boolean check(SecurityIdentity securityIdentity) {
            return false;
        }
    }
}
