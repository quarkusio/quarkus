package io.quarkus.security.test.permissionsallowed;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.test.QuarkusUnitTest;

public class PermitAllWithPermissionsAllowedValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setExpectedException(IllegalStateException.class);

    @Test
    public void test() {
        Assertions.fail();
    }

    @PermissionsAllowed(value = "ignored")
    @PermitAll
    @Singleton
    public static class SecuredBean {

        public void securedBean() {
            // EMPTY
        }

    }
}
