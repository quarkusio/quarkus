package io.quarkus.security.test.permissionsallowed;

import java.security.BasicPermission;
import java.util.UUID;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.test.QuarkusUnitTest;

public class UnknownParamPermissionsAllowedValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .assertException(t -> {
                Assertions.assertEquals(RuntimeException.class, t.getClass(), t.getMessage());
                Assertions.assertTrue(t.getMessage().contains("Parameter 'id' specified via @PermissionsAllowed#params"));
                Assertions.assertTrue(t.getMessage().contains("SecuredBean#securedBean"));
                Assertions.assertTrue(t.getMessage().contains("cannot be matched to any constructor"));
                Assertions.assertTrue(t.getMessage().contains("OrganizationUnitIdPermission' parameter"));
            });

    @Test
    public void test() {
        Assertions.fail();
    }

    @Singleton
    public static class SecuredBean {

        @PermissionsAllowed(value = "ignored", params = "id", permission = OrganizationUnitIdPermission.class)
        public void securedBean(UUID aOrganizationUnitId) {
            // EMPTY
        }

    }

    public static class OrganizationUnitIdPermission extends BasicPermission {

        public OrganizationUnitIdPermission(String name, UUID aOrganizationUnitId) {
            super(name);
        }
    }
}
