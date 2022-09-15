package io.quarkus.keycloak.pep.runtime;

import java.security.Permission;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.keycloak.AuthorizationContext;
import org.keycloak.adapters.authorization.KeycloakAdapterPolicyEnforcer;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.EnforcementMode;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class KeycloakPolicyEnforcerAuthorizer
        implements HttpSecurityPolicy, BiFunction<RoutingContext, SecurityIdentity, HttpSecurityPolicy.CheckResult> {
    private static final String TENANT_ID_ATTRIBUTE = "tenant-id";
    private static final String PERMISSIONS_ATTRIBUTE = "permissions";

    @Inject
    PolicyEnforcerResolver resolver;

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return requestContext.runBlocking(request, identity, this);
    }

    @Override
    public CheckResult apply(RoutingContext routingContext, SecurityIdentity identity) {

        if (identity.isAnonymous()) {
            PathConfig pathConfig = resolver.getPolicyEnforcer(null).getPathMatcher().matches(routingContext.request().path());
            if (pathConfig != null && pathConfig.getEnforcementMode() == EnforcementMode.ENFORCING) {
                return CheckResult.DENY;
            }
        }

        AccessTokenCredential credential = identity.getCredential(AccessTokenCredential.class);

        if (credential == null) {
            // SecurityIdentity has been created by the authentication mechanism other than quarkus-oidc
            return CheckResult.PERMIT;
        }

        VertxHttpFacade httpFacade = new VertxHttpFacade(routingContext, credential.getToken(), resolver.getReadTimeout());

        KeycloakAdapterPolicyEnforcer adapterPolicyEnforcer = new KeycloakAdapterPolicyEnforcer(
                resolver.getPolicyEnforcer(identity.getAttribute(TENANT_ID_ATTRIBUTE)));
        AuthorizationContext result = adapterPolicyEnforcer.authorize(httpFacade);

        if (result.isGranted()) {
            SecurityIdentity newIdentity = enhanceSecurityIdentity(identity, result);
            return new CheckResult(true, newIdentity);
        }

        return CheckResult.DENY;
    }

    @Produces
    @RequestScoped
    public AuthzClient getAuthzClient() {
        SecurityIdentity identity = (SecurityIdentity) Arc.container().instance(SecurityIdentity.class).get();
        return resolver.getPolicyEnforcer(identity.getAttribute(TENANT_ID_ATTRIBUTE)).getClient();
    }

    private SecurityIdentity enhanceSecurityIdentity(SecurityIdentity current,
            AuthorizationContext context) {
        Map<String, Object> attributes = new HashMap<>(current.getAttributes());

        if (context != null) {
            attributes.put(PERMISSIONS_ATTRIBUTE, context.getPermissions());
        }

        return new QuarkusSecurityIdentity.Builder()
                .addAttributes(attributes)
                .setPrincipal(current.getPrincipal())
                .addRoles(current.getRoles())
                .addCredentials(current.getCredentials())
                .addPermissionChecker(new Function<Permission, Uni<Boolean>>() {
                    @Override
                    public Uni<Boolean> apply(Permission permission) {
                        if (context != null) {
                            String scopes = permission.getActions();

                            if (scopes == null || scopes.isEmpty()) {
                                return Uni.createFrom().item(context.hasResourcePermission(permission.getName()));
                            }

                            for (String scope : scopes.split(",")) {
                                if (!context.hasPermission(permission.getName(), scope)) {
                                    return Uni.createFrom().item(false);
                                }
                            }

                            return Uni.createFrom().item(true);
                        }

                        return Uni.createFrom().item(false);
                    }
                }).build();
    }
}
