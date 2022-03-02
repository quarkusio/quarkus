package io.quarkus.it.keycloak;

import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.SecurityIdentityAssociation;

@ApplicationScoped
public class SecurityContextExecutor {
    @Inject
    SecurityIdentityAssociation identityAssociation;
    @Inject
    IdentityProviderManager identityProviderManager;

    @ActivateRequestContext
    public <T> T executeUsingIdentity(String token, Supplier<T> supplier) {
        SecurityIdentity originalIdentity = identityAssociation.getIdentity();
        try {
            SecurityIdentity identity = identityProviderManager.authenticateBlocking(
                    new TokenAuthenticationRequest(new AccessTokenCredential(token, null)));
            identityAssociation.setIdentity(identity);
            return supplier.get();
        } finally {
            identityAssociation.setIdentity(originalIdentity);
        }
    }
}
