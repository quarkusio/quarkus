package io.quarkus.security.test.rolesallowed;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class RolesAllowedExpressionFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(RolesAllowedBean.class, IdentityMock.class, AuthData.class, SecurityTestUtils.class))
            .assertException(t -> {
                Throwable e = t;
                NoSuchElementException te = null;
                while (e != null) {
                    if (e instanceof NoSuchElementException) {
                        te = (NoSuchElementException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(te);
                // assert
                assertTrue(te.getMessage().contains("Could not expand value admin"), te.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }

    @Singleton
    public static class RolesAllowedBean {

        @RolesAllowed("${admin}")
        public final String admin() {
            return "accessibleForAdminOnly";
        }

    }

}
