package io.quarkus.security.test.permissionsallowed;

import java.security.BasicPermission;
import java.util.UUID;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.test.QuarkusUnitTest;

public class UnusedParamPermissionsAllowedValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .assertException(t -> {
                Assertions.assertEquals(RuntimeException.class, t.getClass(), t.getMessage());
                Assertions.assertTrue(t.getMessage().contains("nestedParam1.something"));
                Assertions.assertTrue(t.getMessage().contains("cannot be matched to any constructor"));
                Assertions.assertTrue(t.getMessage().contains("OrganizationUnitIdPermission' parameter"));
            });

    @Test
    public void test() {
        Assertions.fail();
    }

    @Singleton
    public static class SecuredBean {

        @PermissionsAllowed(value = "ignored", params = { "aOrganizationUnitId",
                "nestedParam1.something" }, permission = OrganizationUnitIdPermission.class)
        public void securedBean(UUID aOrganizationUnitId, NestedParam1 nestedParam1) {
            // EMPTY
        }

    }

    public static class NestedParam1 {
        final String something;

        public NestedParam1(String something) {
            this.something = something;
        }
    }

    public static class OrganizationUnitIdPermission extends BasicPermission {

        public OrganizationUnitIdPermission(String name, UUID aOrganizationUnitId) {
            super(name);
        }
    }
}
