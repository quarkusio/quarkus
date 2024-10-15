package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusUnitTest;

public class CheckerNotCdiBeanValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .assertException(t -> {
                Assertions.assertEquals(RuntimeException.class, t.getClass(), t.getMessage());
                Assertions.assertTrue(t.getMessage().contains("@PermissionChecker declared on method 'checkSomeValue'"));
                Assertions.assertTrue(t.getMessage().contains("no matching CDI bean could be found"));
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

    }

    public static class Checker {

        @PermissionChecker("some-value")
        boolean checkSomeValue(SecurityIdentity identity) {
            return false;
        }

    }

}
