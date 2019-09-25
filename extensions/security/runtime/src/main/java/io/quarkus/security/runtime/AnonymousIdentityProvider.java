package io.quarkus.security.runtime;

import java.security.Permission;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;

public class AnonymousIdentityProvider implements IdentityProvider<AnonymousAuthenticationRequest> {

    private static final Principal principal = new Principal() {
        @Override
        public String getName() {
            return "";
        }
    };

    private static final SecurityIdentity instance = new SecurityIdentity() {
        @Override
        public Principal getPrincipal() {
            return principal;
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
        public CompletionStage<Boolean> checkPermission(Permission permission) {
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            cf.complete(false);
            return cf;
        }
    };

    @Override
    public Class<AnonymousAuthenticationRequest> getRequestType() {
        return AnonymousAuthenticationRequest.class;
    }

    @Override
    public CompletionStage<SecurityIdentity> authenticate(AnonymousAuthenticationRequest request,
            AuthenticationRequestContext context) {
        CompletableFuture<SecurityIdentity> cf = new CompletableFuture<>();
        cf.complete(instance);
        return cf;
    }
}
