package io.quarkus.security.test.runasuser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.RunAsUser;
import io.quarkus.security.spi.RunAsUserPredicateBuildItem;
import io.quarkus.test.QuarkusUnitTest;

@ActivateRequestContext
class RunAsUserSecurityAnnotationsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClass(SecuredBean.class))
            .addBuildChainCustomizer(b -> b
                    .addBuildStep(context -> context.produce(RunAsUserPredicateBuildItem.ofAnnotation(Test.class)))
                    .produces(RunAsUserPredicateBuildItem.class).build());

    @Inject
    SecuredBean securedBean;

    @RunAsUser(user = "Martin")
    @Test
    void testAuthenticated() {
        assertDoesNotThrow(securedBean::authenticated);
        assertThrows(ForbiddenException.class, securedBean::rolesAllowedAdmin);
    }

    @RunAsUser(user = "Merlin", roles = { "admin", "user" })
    @Test
    void testRolesAllowed() {
        assertDoesNotThrow(securedBean::rolesAllowedAdmin);
        assertDoesNotThrow(securedBean::rolesAllowedUser);
        assertThrows(ForbiddenException.class, securedBean::rolesAllowedOther);
    }

    @ApplicationScoped
    static class SecuredBean {

        @Authenticated
        void authenticated() {

        }

        @RolesAllowed("admin")
        void rolesAllowedAdmin() {

        }

        @RolesAllowed("user")
        void rolesAllowedUser() {

        }

        @RolesAllowed("other")
        void rolesAllowedOther() {

        }

    }
}
