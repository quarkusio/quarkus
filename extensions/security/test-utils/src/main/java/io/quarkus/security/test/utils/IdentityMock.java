package io.quarkus.security.test.utils;

import java.security.Permission;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.BaseAuthenticationRequest;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.smallrye.mutiny.Uni;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Alternative
@ApplicationScoped
@Priority(1)
public class IdentityMock implements SecurityIdentity {

    public static final AuthData ANONYMOUS = new AuthData(null, true, null, null);
    public static final AuthData USER = new AuthData(Collections.singleton("user"), false, "user", Set.of());
    public static final AuthData ADMIN = new AuthData(Collections.singleton("admin"), false, "admin", Set.of());

    private static volatile boolean anonymous;
    private static volatile Set<String> roles;
    private static volatile Set<Permission> permissions = new HashSet<>();
    private static volatile String name;
    private static volatile boolean applyAugmentors;

    public static void setUpAuth(AuthData auth) {
        IdentityMock.anonymous = auth.anonymous;
        IdentityMock.roles = auth.roles;
        IdentityMock.name = auth.name;
        IdentityMock.permissions = auth.permissions == null ? Set.of() : auth.permissions;
        IdentityMock.applyAugmentors = auth.applyAugmentors;
    }

    @Override
    public Principal getPrincipal() {
        return new Principal() {
            @Override
            public String getName() {
                return name;
            }
        };
    }

    @Override
    public boolean hasRole(String role) {
        if (roles == null) {
            return false;
        }
        return getRoles().contains(role);
    }

    @Override
    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public <T extends Credential> T getCredential(Class<T> aClass) {
        return null;
    }

    @Override
    public Set<Credential> getCredentials() {
        return Set.of();
    }

    @Override
    public <T> T getAttribute(String s) {
        return null;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    @Override
    public Uni<Boolean> checkPermission(Permission permission) {
        final boolean permitted = permission != null && permissions.stream().anyMatch(p -> p.implies(permission));
        return Uni.createFrom().item(permitted);
    }

    @Alternative
    @ApplicationScoped
    @Priority(1)
    public static class IdentityAssociationMock extends SecurityIdentityAssociation {

        @Inject
        IdentityMock identity;

        @Inject
        IdentityProviderManager identityProviderManager;

        @Override
        public Uni<SecurityIdentity> getDeferredIdentity() {
            if (applyAugmentors) {
                return identityProviderManager.authenticate(new IdentityMockAuthenticationRequest());
            }
            return Uni.createFrom().item(identity);
        }

        @Override
        public SecurityIdentity getIdentity() {
            if (applyAugmentors) {
                return getDeferredIdentity().await().indefinitely();
            }
            return identity;
        }

    }

    public static final class IdentityMockAuthenticationRequest extends BaseAuthenticationRequest {

    }

    @ApplicationScoped
    public static final class IdentityMockProvider implements IdentityProvider<IdentityMockAuthenticationRequest> {

        @Inject
        IdentityMock identity;

        @Override
        public Class<IdentityMockAuthenticationRequest> getRequestType() {
            return IdentityMockAuthenticationRequest.class;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(IdentityMockAuthenticationRequest identityMockAuthenticationRequest,
                AuthenticationRequestContext authenticationRequestContext) {
            return Uni.createFrom().item(identity);
        }
    }
}
