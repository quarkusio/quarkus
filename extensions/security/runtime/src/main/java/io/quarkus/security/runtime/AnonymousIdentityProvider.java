package io.quarkus.security.runtime;

import java.security.Permission;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.smallrye.mutiny.Uni;

public class AnonymousIdentityProvider implements IdentityProvider<AnonymousAuthenticationRequest> {

    private static final Principal PRINCIPAL = new Principal() {
        @Override
        public String getName() {
            return "";
        }
    };

    private static final SecurityIdentity INSTANCE = new SecurityIdentity() {
        @Override
        public Principal getPrincipal() {
            return PRINCIPAL;
        }

        @Override
        public boolean isAnonymous() {
            return true;
        }

        @Override
        public Set<String> getRoles() {
            return Collections.emptySet();
        }

        @Override
        public boolean hasRole(String role) {
            return false;
        }

        @Override
        public <T extends Credential> T getCredential(Class<T> credentialType) {
            return null;
        }

        @Override
        public Set<Credential> getCredentials() {
            return Collections.emptySet();
        }

        @Override
        public <T> T getAttribute(String name) {
            return null;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public Uni<Boolean> checkPermission(Permission permission) {
            return Uni.createFrom().item(false);
        }
    };

    @Override
    public Class<AnonymousAuthenticationRequest> getRequestType() {
        return AnonymousAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(AnonymousAuthenticationRequest request,
            AuthenticationRequestContext context) {
        return Uni.createFrom().item(INSTANCE);
    }
}
