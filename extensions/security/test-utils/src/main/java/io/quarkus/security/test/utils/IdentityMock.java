package io.quarkus.security.test.utils;

import java.security.Permission;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.smallrye.mutiny.Uni;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Alternative
@ApplicationScoped
@Priority(1)
public class IdentityMock implements SecurityIdentity {

    public static final AuthData ANONYMOUS = new AuthData(null, true, null);
    public static final AuthData USER = new AuthData(Collections.singleton("user"), false, "user");
    public static final AuthData ADMIN = new AuthData(Collections.singleton("admin"), false, "admin");

    private static volatile boolean anonymous;
    private static volatile Set<String> roles;
    private static volatile String name;

    public static void setUpAuth(AuthData auth) {
        IdentityMock.anonymous = auth.anonymous;
        IdentityMock.roles = auth.roles;
        IdentityMock.name = auth.name;
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
        return null;
    }

    @Override
    public <T> T getAttribute(String s) {
        return null;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Uni<Boolean> checkPermission(Permission permission) {
        return null;
    }

    @Alternative
    @ApplicationScoped
    @Priority(1)
    public static class IdentityAssociationMock extends SecurityIdentityAssociation {
        @Inject
        IdentityMock identity;

        @Override
        public Uni<SecurityIdentity> getDeferredIdentity() {
            return Uni.createFrom().item(identity);
        }

        @Override
        public SecurityIdentity getIdentity() {
            return identity;
        }
    }
}
