package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class TestSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

    private static volatile boolean invoked = false;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
            AuthenticationRequestContext authenticationRequestContext) {
        invoked = true;
        final SecurityIdentity identity;
        if (securityIdentity.isAnonymous() || !"authorized-user".equals(securityIdentity.getPrincipal().getName())) {
            identity = securityIdentity;
        } else {
            identity = QuarkusSecurityIdentity.builder(securityIdentity)
                    .addPermission(new CustomPermission("augmented")).build();
        }
        return Uni.createFrom().item(identity);
    }

    public static boolean isInvoked() {
        return invoked;
    }

    public static void resetInvoked() {
        invoked = false;
    }
}
