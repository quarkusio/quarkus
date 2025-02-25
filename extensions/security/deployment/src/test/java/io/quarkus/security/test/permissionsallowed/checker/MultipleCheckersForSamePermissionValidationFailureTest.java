package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleCheckersForSamePermissionValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .assertException(t -> {
                Assertions.assertEquals(IllegalArgumentException.class, t.getClass(), t.getMessage());
                Assertions.assertTrue(
                        t.getMessage().contains("Detected two @PermissionChecker annotations with same value 'some-value'"));
            });

    @Test
    public void test() {
        Assertions.fail();
    }

    @Singleton
    public static class SecuredBean {

        @PermissionsAllowed("some-value")
        void securedBean() {
            // EMPTY
        }

        @PermissionChecker("some-value")
        boolean checkSomeValue(SecurityIdentity identity) {
            return false;
        }

        @PermissionChecker("some-value")
        boolean alsoCheckSomeValue(SecurityIdentity identity) {
            return false;
        }
    }

}
