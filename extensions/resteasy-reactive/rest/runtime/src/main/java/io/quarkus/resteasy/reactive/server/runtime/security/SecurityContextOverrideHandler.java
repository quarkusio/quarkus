package io.quarkus.resteasy.reactive.server.runtime.security;

import java.security.Permission;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.ws.rs.core.SecurityContext;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.arc.Arc;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveSecurityContext;
import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class SecurityContextOverrideHandler implements ServerRestHandler {

    private static final SecurityContextOverrideHandler INSTANCE = new SecurityContextOverrideHandler();

    private SecurityContextOverrideHandler() {
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        if (!requestContext.isSecurityContextSet()) {
            //nothing to do
            return;
        }
        SecurityContext modified = requestContext.getSecurityContext();
        if (modified instanceof ResteasyReactiveSecurityContext) {
            //an original security context, it has not been modified
            return;
        }
        updateIdentity(requestContext, modified);
    }

    private static void updateIdentity(ResteasyReactiveRequestContext requestContext, SecurityContext modified) {
        requestContext.requireCDIRequestScope();
        final CurrentIdentityAssociation currentIdentityAssociation = getIdentityAssociation();
        if (currentIdentityAssociation != null) {
            RoutingContext routingContext = requestContext.unwrap(RoutingContext.class);
            Uni<SecurityIdentity> oldIdentity = currentIdentityAssociation.getDeferredIdentity();
            currentIdentityAssociation.setIdentity(oldIdentity.map(new Function<SecurityIdentity, SecurityIdentity>() {
                @Override
                public SecurityIdentity apply(SecurityIdentity old) {
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

                        @SuppressWarnings("unchecked")
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

                        @SuppressWarnings("unchecked")
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
                    if (routingContext != null) {
                        routingContext.setUser(new QuarkusHttpUser(newIdentity));
                    }
                    return newIdentity;
                }
            }));
        }
    }

    private static CurrentIdentityAssociation getIdentityAssociation() {
        if (EagerSecurityContext.instance != null) {
            return EagerSecurityContext.instance.identityAssociation.orElse(null);
        }
        // this should only happen when Quarkus Security extension is not present
        // but user implements security themselves, like in their own JAX-RS filter
        return Arc.container().instance(CurrentIdentityAssociation.class).orElse(null);
    }

    public static class Customizer implements HandlerChainCustomizer {
        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
                ServerResourceMethod serverResourceMethod) {
            if (phase == Phase.AFTER_PRE_MATCH) {
                return Collections.singletonList(INSTANCE);
            }
            return Collections.emptyList();
        }
    }
}
