package io.quarkus.email.authentication.deployment.test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
class TestEmailIdentityAugmentor implements SecurityIdentityAugmentor {

    private record UserDetail(String email, Set<String> roles) {
    }

    private static final Map<String, UserDetail> knownUsers = new ConcurrentHashMap<>();

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity, AuthenticationRequestContext reqCtx) {
        if (!securityIdentity.isAnonymous()) {
            String principalName = securityIdentity.getPrincipal().getName();
            if (knownUsers.containsKey(principalName)) {
                return Uni.createFrom().item(QuarkusSecurityIdentity
                        .builder(securityIdentity)
                        .addRoles(knownUsers.get(principalName).roles)
                        .build());
            }
        }
        return Uni.createFrom().item(securityIdentity);
    }

    static void reset() {
        knownUsers.clear();
    }

    static Builder addUser(String email, String... roles) {
        return new Builder().addUser(email, roles);
    }

    static final class Builder {

        Builder addUser(String email, String... roles) {
            knownUsers.put(email, new UserDetail(email, Set.of(roles)));
            return this;
        }

    }
}
