package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.test.QuarkusUnitTest;

public class NoArgsPermissionCheckerValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .assertException(t -> {
                Assertions.assertEquals(RuntimeException.class, t.getClass(), t.getMessage());
                Assertions.assertTrue(t.getMessage().contains("Checker#checkSomeValue"));
                Assertions.assertTrue(t.getMessage().contains("must have at least one parameter"));
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

    @Singleton
    public static class Checker {

        @PermissionChecker("some-value")
        boolean checkSomeValue() {
            return false;
        }

    }

}
