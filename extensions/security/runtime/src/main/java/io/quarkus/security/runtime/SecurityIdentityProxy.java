package io.quarkus.security.runtime;

import java.security.Permission;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@RequestScoped
public class SecurityIdentityProxy implements SecurityIdentity {

    @Inject
    SecurityIdentityAssociation association;

    @Override
    public Principal getPrincipal() {
        return association.getIdentity().getPrincipal();
    }

    @Override
    public boolean isAnonymous() {
        return association.getIdentity().isAnonymous();
    }

    @Override
    public Set<String> getRoles() {
        return association.getIdentity().getRoles();
    }

    @Override
    public boolean hasRole(String role) {
        return association.getIdentity().hasRole(role);
    }

    @Override
    public <T extends Credential> T getCredential(Class<T> credentialType) {
        return association.getIdentity().getCredential(credentialType);
    }

    @Override
    public Set<Credential> getCredentials() {
        return association.getIdentity().getCredentials();
    }

    @Override
    public <T> T getAttribute(String name) {
        return association.getIdentity().getAttribute(name);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return association.getIdentity().getAttributes();
    }

    @Override
    public Uni<Boolean> checkPermission(Permission permission) {
        return association.getIdentity().checkPermission(permission);
    }

    @Override
    public boolean checkPermissionBlocking(Permission permission) {
        return association.getIdentity().checkPermissionBlocking(permission);
    }
}
