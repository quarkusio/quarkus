package io.quarkus.security.test.runasuser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.Principal;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.RunAsUser;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.RunAsUserPredicateBuildItem;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

@ActivateRequestContext
class RunAsUserSecurityBeansTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(WhoAmIBean.class, FakeScheduled.class))
            .addBuildChainCustomizer(b -> b
                    .addBuildStep(context -> context.produce(RunAsUserPredicateBuildItem.ofAnnotation(FakeScheduled.class)))
                    .produces(RunAsUserPredicateBuildItem.class).build());

    @Inject
    WhoAmIBean whoAmIBean;

    @BeforeEach
    void resetReturnValues() {
        whoAmIBean.reset();
    }

    @Test
    void testPrincipalName_Wrapper() {
        // return type: Void
        whoAmIBean.getFromPrincipal_Wrapper();
        assertEquals("Mirek", whoAmIBean.getReturnedPrincipalName());
    }

    @Test
    void testPrincipalName() {
        // return type: void
        whoAmIBean.getFromPrincipal();
        assertEquals("Mirek", whoAmIBean.getReturnedPrincipalName());
    }

    @Test
    void testSecurityIdentity() {
        whoAmIBean.getFromIdentityWithoutRoles();
        assertEquals("Marek", whoAmIBean.getReturnedPrincipalName());
        assertEquals(0, whoAmIBean.getReturnedRoles().size());
    }

    @Test
    void testSecurityIdentityRoles() {
        whoAmIBean.getFromIdentityWithRoles();
        assertEquals("Milan", whoAmIBean.getReturnedPrincipalName());
        var roles = whoAmIBean.getReturnedRoles();
        assertEquals(2, roles.size());
        assertTrue(roles.contains("user"));
        assertTrue(roles.contains("admin"));
    }

    @Test
    void testCurrentIdentityAssociation() {
        whoAmIBean.getFromCurrentIdentityAssociation();
        assertEquals("Michal", whoAmIBean.getReturnedPrincipalName());
    }

    @Test
    void testPrincipalName_Uni() {
        whoAmIBean.getFromPrincipal_Uni().await().indefinitely();
        assertEquals("Mirek", whoAmIBean.getReturnedPrincipalName());
    }

    @Test
    void testSecurityIdentity_Uni() {
        whoAmIBean.getFromIdentityWithoutRoles_Uni().await().indefinitely();
        assertEquals("Marek", whoAmIBean.getReturnedPrincipalName());
        assertEquals(0, whoAmIBean.getReturnedRoles().size());
    }

    @Test
    void testSecurityIdentityRoles_Uni() {
        whoAmIBean.getFromIdentityWithRoles_Uni().await().indefinitely();
        assertEquals("Milan", whoAmIBean.getReturnedPrincipalName());
        var roles = whoAmIBean.getReturnedRoles();
        assertEquals(2, roles.size());
        assertTrue(roles.contains("user"));
        assertTrue(roles.contains("admin"));
    }

    @Test
    void testCurrentIdentityAssociation_Uni() {
        whoAmIBean.getFromCurrentIdentityAssociation_Uni().await().indefinitely();
        assertEquals("Michal", whoAmIBean.getReturnedPrincipalName());
        assertEquals(0, whoAmIBean.getReturnedRoles().size());
    }

    @Test
    void testCurrentIdentityAssociation_CompletionStage() {
        whoAmIBean.getFromCurrentIdentityAssociation_CompletionStage().toCompletableFuture().join();
        assertEquals("Michal", whoAmIBean.getReturnedPrincipalName());
        assertEquals(0, whoAmIBean.getReturnedRoles().size());
    }

    @ApplicationScoped
    static class WhoAmIBean {

        private volatile String returnedPrincipalName;
        private volatile Set<String> returnedRoles;

        @Inject
        Principal principal;

        @Inject
        SecurityIdentity securityIdentity;

        @Inject
        CurrentIdentityAssociation currentIdentityAssociation;

        @FakeScheduled
        @RunAsUser(user = "Mirek")
        Void getFromPrincipal_Wrapper() {
            returnedPrincipalName = principal.getName();
            returnedRoles = null;
            return null;
        }

        @FakeScheduled
        @RunAsUser(user = "Mirek")
        void getFromPrincipal() {
            returnedPrincipalName = principal.getName();
            returnedRoles = null;
        }

        @FakeScheduled
        @RunAsUser(user = "Mirek")
        Uni<Void> getFromPrincipal_Uni() {
            return Uni.createFrom().item(() -> {
                returnedPrincipalName = principal.getName();
                returnedRoles = Set.of();
                return null;
            });
        }

        @FakeScheduled
        @RunAsUser(user = "Marek")
        void getFromIdentityWithoutRoles() {
            returnedPrincipalName = securityIdentity.getPrincipal().getName();
            returnedRoles = securityIdentity.getRoles();
        }

        @FakeScheduled
        @RunAsUser(user = "Marek")
        Uni<Void> getFromIdentityWithoutRoles_Uni() {
            return Uni.createFrom().item(() -> {
                returnedPrincipalName = securityIdentity.getPrincipal().getName();
                returnedRoles = securityIdentity.getRoles();
                return null;
            });
        }

        @FakeScheduled
        @RunAsUser(user = "Milan", roles = { "user", "admin" })
        void getFromIdentityWithRoles() {
            returnedPrincipalName = securityIdentity.getPrincipal().getName();
            returnedRoles = securityIdentity.getRoles();
        }

        @FakeScheduled
        @RunAsUser(user = "Milan", roles = { "user", "admin" })
        Uni<Void> getFromIdentityWithRoles_Uni() {
            return Uni.createFrom().item(() -> {
                returnedPrincipalName = securityIdentity.getPrincipal().getName();
                returnedRoles = securityIdentity.getRoles();
                return null;
            });
        }

        @FakeScheduled
        @RunAsUser(user = "Michal")
        void getFromCurrentIdentityAssociation() {
            var identity = currentIdentityAssociation.getIdentity();
            returnedPrincipalName = identity.getPrincipal().getName();
            returnedRoles = identity.getRoles();
        }

        @FakeScheduled
        @RunAsUser(user = "Michal")
        Uni<Void> getFromCurrentIdentityAssociation_Uni() {
            return touchDeferredIdentity();
        }

        @FakeScheduled
        @RunAsUser(user = "Michal")
        CompletionStage<Void> getFromCurrentIdentityAssociation_CompletionStage() {
            return touchDeferredIdentity().subscribeAsCompletionStage();
        }

        private Uni<Void> touchDeferredIdentity() {
            return currentIdentityAssociation.getDeferredIdentity()
                    .invoke(identity -> {
                        returnedPrincipalName = identity.getPrincipal().getName();
                        returnedRoles = identity.getRoles();
                    }).replaceWithVoid();
        }

        String getReturnedPrincipalName() {
            return returnedPrincipalName;
        }

        Set<String> getReturnedRoles() {
            return returnedRoles;
        }

        void reset() {
            returnedRoles = Set.of();
            returnedPrincipalName = null;
        }
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @interface FakeScheduled {
    }
}
