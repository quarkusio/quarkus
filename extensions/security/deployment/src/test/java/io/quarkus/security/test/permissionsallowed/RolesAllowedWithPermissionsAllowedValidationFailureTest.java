package io.quarkus.security.test.permissionsallowed;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.test.QuarkusUnitTest;

public class RolesAllowedWithPermissionsAllowedValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setExpectedException(IllegalStateException.class);

    @Test
    public void test() {
        Assertions.fail();
    }

    @Singleton
    public static class SecuredBean {

        @RolesAllowed("ignored")
        @PermissionsAllowed(value = "ignored")
        public void securedBean() {
            // EMPTY
        }

    }
}
