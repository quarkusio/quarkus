package io.quarkus.vertx.http.runtime.security;

import java.security.Permission;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

final class RoutingContextAwareSecurityIdentity implements SecurityIdentity {

    private static final String ROUTING_CONTEXT_KEY = RoutingContext.class.getName();
    private final SecurityIdentity delegate;
    private final RoutingContext routingContext;

    private RoutingContextAwareSecurityIdentity(SecurityIdentity delegate, RoutingContext routingContext) {
        this.delegate = delegate;
        this.routingContext = routingContext;
    }

    static SecurityIdentity addRoutingCtxToIdentityIfMissing(SecurityIdentity delegate, RoutingContext routingContext) {
        if (delegate != null && delegate.getAttribute(ROUTING_CONTEXT_KEY) == null) {
            return new RoutingContextAwareSecurityIdentity(delegate, routingContext);
        }
        return delegate;
    }

    @Override
    public Principal getPrincipal() {
        return delegate.getPrincipal();
    }

    @Override
    public boolean isAnonymous() {
        return delegate.isAnonymous();
    }

    @Override
    public Set<String> getRoles() {
        return delegate.getRoles();
    }

    @Override
    public boolean hasRole(String s) {
        return delegate.hasRole(s);
    }

    @Override
    public <T extends Credential> T getCredential(Class<T> aClass) {
        return delegate.getCredential(aClass);
    }

    @Override
    public Set<Credential> getCredentials() {
        return delegate.getCredentials();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttribute(String s) {
        if (ROUTING_CONTEXT_KEY.equals(s)) {
            return (T) routingContext;
        }
        return delegate.getAttribute(s);
    }

    @Override
    public Map<String, Object> getAttributes() {
        // we always recreate the map as it could have changed in the delegate
        var delegateAttributes = delegate.getAttributes();
        if (delegateAttributes == null || delegateAttributes.isEmpty()) {
            return Map.of(ROUTING_CONTEXT_KEY, routingContext);
        }
        var result = new HashMap<>(delegateAttributes);
        result.put(ROUTING_CONTEXT_KEY, routingContext);
        return result;
    }

    @Override
    public Uni<Boolean> checkPermission(Permission permission) {
        return delegate.checkPermission(permission);
    }
}
