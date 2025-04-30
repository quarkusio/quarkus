package io.quarkus.security.test.permissionsallowed.checker;

import java.util.UUID;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.test.QuarkusUnitTest;

public class UnknownCheckerParamValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .assertException(t -> {
                Assertions.assertEquals(RuntimeException.class, t.getClass(), t.getMessage());
                Assertions.assertTrue(t.getMessage().contains("No '"));
                Assertions.assertTrue(t.getMessage().contains("SecuredBean#securedBean' formal parameter name matches"));
                Assertions.assertTrue(t.getMessage().contains("SecuredBean#check"));
                Assertions.assertTrue(t.getMessage().contains("parameter name 'unknownParameter'"));
            });

    @Test
    public void test() {
        Assertions.fail();
    }

    @Singleton
    public static class SecuredBean {

        @PermissionsAllowed("checker")
        public void securedBean(UUID aOrganizationUnitId) {
            // EMPTY
        }

        @PermissionChecker("checker")
        public boolean check(String unknownParameter) {
            return false;
        }
    }
}
