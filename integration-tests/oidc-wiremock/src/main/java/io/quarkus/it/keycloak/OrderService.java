package io.quarkus.it.keycloak;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.Tenant;
import io.quarkus.oidc.TenantIdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;

@ApplicationScoped
public class OrderService {

    static final Set<String> IDENTITY_REPOSITORY = ConcurrentHashMap.newKeySet();

    @Inject
    SecurityIdentity identity;

    @Tenant("bearer")
    @Inject
    TenantIdentityProvider identityProvider;

    @Blocking
    @ConsumeEvent("product-order")
    void processOrder(Product product) {
        if (!identity.isAnonymous()) {
            // this condition establish need for OIDCIdentityProvider with pre-selected tenant
            throw new IllegalStateException("Event is expected to be consumed on new request context");
        }
        var principal = identityProvider.authenticate(new AccessTokenCredential(product.accessToken)).await().indefinitely()
                .getPrincipal().getName();
        IDENTITY_REPOSITORY.add(principal);
    }

}
