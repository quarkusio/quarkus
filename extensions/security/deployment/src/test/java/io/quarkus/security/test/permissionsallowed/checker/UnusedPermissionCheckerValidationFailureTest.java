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

public class UnusedPermissionCheckerValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .assertException(t -> {
                var exceptionMessage = t.getMessage();
                assertEquals(RuntimeException.class, t.getClass(), exceptionMessage);
                assertTrue(
                        t.getMessage().contains(
                                "Found @PermissionChecker annotation instance that authorize the 'checker' permission"),
                        exceptionMessage);
                assertTrue(t.getMessage().contains("no @PermissionsAllowed annotation instance requires this permission"),
                        exceptionMessage);
            });

    @Test
    public void test() {
        Assertions.fail();
    }

    @Singleton
    public static class SecuredBean {

        @PermissionsAllowed("not-a-checker")
        public void securedBean() {
            // EMPTY
        }

        @PermissionChecker("checker")
        public boolean check(SecurityIdentity securityIdentity) {
            return false;
        }
    }
}
