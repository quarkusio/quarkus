package io.quarkus.resteasy.runtime;

import java.io.IOException;
import java.security.Permission;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.server.servlet.ServletSecurityContext;

import io.quarkus.resteasy.runtime.standalone.QuarkusResteasySecurityContext;
import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@PreMatching
@Priority(Priorities.USER + 1)
@Provider
public class SecurityContextFilter implements ContainerRequestFilter {

    @Inject
    SecurityIdentity old;

    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        SecurityContext modified = requestContext.getSecurityContext();
        if (modified instanceof ServletSecurityContext || modified instanceof QuarkusResteasySecurityContext) {
            //an original security context, it has not been modified
            return;
        }
        Set<Credential> oldCredentials = old.getCredentials();
        Map<String, Object> oldAttributes = old.getAttributes();
        SecurityIdentity newIdentity = new SecurityIdentity() {
            @Override
            public Principal getPrincipal() {
                return modified.getUserPrincipal();
            }

            @Override
            public boolean isAnonymous() {
                return modified.getUserPrincipal() == null;
            }

            @Override
            public Set<String> getRoles() {
                throw new UnsupportedOperationException(
                        "retrieving all roles not supported when JAX-RS security context has been replaced");
            }

            @Override
            public boolean hasRole(String role) {
                return modified.isUserInRole(role);
            }

            @Override
            public <T extends Credential> T getCredential(Class<T> credentialType) {
                for (Credential cred : getCredentials()) {
                    if (credentialType.isAssignableFrom(cred.getClass())) {
                        return (T) cred;
                    }
                }
                return null;
            }

            @Override
            public Set<Credential> getCredentials() {
                return oldCredentials;
            }

            @Override
            public <T> T getAttribute(String name) {
                return (T) oldAttributes.get(name);
            }

            @Override
            public Map<String, Object> getAttributes() {
                return oldAttributes;
            }

            @Override
            public Uni<Boolean> checkPermission(Permission permission) {
                return Uni.createFrom().nullItem();
            }
        };
        currentIdentityAssociation.setIdentity(newIdentity);
    }
}
