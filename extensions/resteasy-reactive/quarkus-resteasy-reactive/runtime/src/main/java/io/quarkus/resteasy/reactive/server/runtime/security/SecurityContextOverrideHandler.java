package io.quarkus.resteasy.reactive.server.runtime.security;

import java.security.Permission;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.core.SecurityContext;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveSecurityContext;
import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

public class SecurityContextOverrideHandler implements ServerRestHandler {

    private volatile InjectableInstance<CurrentIdentityAssociation> currentIdentityAssociation;

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

    private void updateIdentity(ResteasyReactiveRequestContext requestContext, SecurityContext modified) {
        requestContext.requireCDIRequestScope();
        InjectableInstance<CurrentIdentityAssociation> instance = getCurrentIdentityAssociation();
        if (instance.isResolvable()) {
            CurrentIdentityAssociation currentIdentityAssociation = instance.get();
            Uni<SecurityIdentity> oldIdentity = currentIdentityAssociation.getDeferredIdentity();
            currentIdentityAssociation.setIdentity(oldIdentity.map(new Function<SecurityIdentity, SecurityIdentity>() {
                @Override
                public SecurityIdentity apply(SecurityIdentity old) {
                    Set<Credential> oldCredentials = old.getCredentials();
                    Map<String, Object> oldAttributes = old.getAttributes();
                    return new SecurityIdentity() {
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
                }
            }));
        }
    }

    private InjectableInstance<CurrentIdentityAssociation> getCurrentIdentityAssociation() {
        InjectableInstance<CurrentIdentityAssociation> identityAssociation = this.currentIdentityAssociation;
        if (identityAssociation == null) {
            return this.currentIdentityAssociation = Arc.container().select(CurrentIdentityAssociation.class);
        }
        return identityAssociation;
    }

    public static class Customizer implements HandlerChainCustomizer {
        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
                ServerResourceMethod serverResourceMethod) {
            if (phase == Phase.AFTER_PRE_MATCH) {
                return Collections.singletonList(new SecurityContextOverrideHandler());
            }
            return Collections.emptyList();
        }
    }
}
