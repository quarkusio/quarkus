package io.quarkus.keycloak.pep.runtime;

import static io.quarkus.oidc.runtime.OidcUtils.TENANT_ID_ATTRIBUTE;

import java.security.Permission;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.keycloak.AuthorizationContext;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.EnforcementMode;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;

import io.quarkus.arc.Arc;
import io.quarkus.keycloak.pep.PolicyEnforcerResolver;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.BlockingOperationNotAllowedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class KeycloakPolicyEnforcerAuthorizer implements HttpSecurityPolicy {
    private static final String PERMISSIONS_ATTRIBUTE = "permissions";
    private static final String POLICY_ENFORCER = "io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerAuthorizer#POLICY_ENFORCER";

    @Inject
    PolicyEnforcerResolver resolver;

    @Inject
    Instance<SecurityIdentity> identityInstance;

    @Inject
    BlockingSecurityExecutor blockingExecutor;

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext routingContext, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return identity.flatMap(new Function<SecurityIdentity, Uni<? extends CheckResult>>() {
            @Override
            public Uni<? extends CheckResult> apply(SecurityIdentity identity) {
                if (identity.isAnonymous()) {
                    return resolver.resolvePolicyEnforcer(routingContext, null)
                            .flatMap(new Function<PolicyEnforcer, Uni<? extends CheckResult>>() {
                                @Override
                                public Uni<CheckResult> apply(PolicyEnforcer policyEnforcer) {
                                    storePolicyEnforcerOnContext(policyEnforcer, routingContext);
                                    return blockingExecutor.executeBlocking(new Supplier<PathConfig>() {
                                        @Override
                                        public PathConfig get() {
                                            return policyEnforcer.getPathMatcher().matches(routingContext.normalizedPath());
                                        }
                                    }).flatMap(new Function<PathConfig, Uni<? extends CheckResult>>() {
                                        @Override
                                        public Uni<CheckResult> apply(PathConfig pathConfig) {
                                            if (pathConfig != null
                                                    && pathConfig.getEnforcementMode() == EnforcementMode.ENFORCING) {
                                                return CheckResult.deny();
                                            }
                                            return checkPermissionInternal(routingContext, identity);
                                        }
                                    });
                                }
                            });
                }
                return checkPermissionInternal(routingContext, identity);
            }
        });
    }

    @Produces
    @RequestScoped
    public AuthzClient getAuthzClient() {
        SecurityIdentity identity = identityInstance.get();
        final RoutingContext routingContext;
        if (identity.getAttribute(RoutingContext.class.getName()) != null) {
            routingContext = identity.getAttribute(RoutingContext.class.getName());
        } else {
            routingContext = Arc.container().instance(CurrentVertxRequest.class).get().getCurrent();
        }

        if (routingContext != null && routingContext.get(POLICY_ENFORCER) != null) {
            return routingContext.<PolicyEnforcer> get(POLICY_ENFORCER).getAuthzClient();
        } else if (BlockingOperationControl.isBlockingAllowed()) {
            OidcTenantConfig tenantConfig = routingContext == null ? null
                    : routingContext.get(OidcTenantConfig.class.getName());
            return resolver.resolvePolicyEnforcer(routingContext, tenantConfig)
                    .await().indefinitely()
                    .getAuthzClient();
        } else {
            if (resolver instanceof DefaultPolicyEnforcerResolver defaultResolver
                    && !defaultResolver.hasDynamicPolicyEnforcers()) {
                return defaultResolver.getStaticPolicyEnforcer(identity.getAttribute(TENANT_ID_ATTRIBUTE)).getAuthzClient();
            } else {
                // this shouldn't happen inside HTTP request as policy enforcer is in most cases accessible from context
                // and the Authz client itself is blocking so users can as well inject it when on the worker thread
                throw new BlockingOperationNotAllowedException("""
                        You have attempted to inject AuthzClient on a IO thread.
                        This is not allowed when PolicyEnforcer is resolved dynamically as blocking operations are required.
                        Make sure you are injecting AuthzClient from a worker thread.
                        """);
            }
        }
    }

    private Uni<CheckResult> checkPermissionInternal(RoutingContext routingContext, SecurityIdentity identity) {
        AccessTokenCredential credential = identity.getCredential(AccessTokenCredential.class);

        if (credential == null) {
            // SecurityIdentity has been created by the authentication mechanism other than quarkus-oidc
            return CheckResult.permit();
        }

        VertxHttpFacade httpFacade = new VertxHttpFacade(routingContext, credential.getToken(), resolver.getReadTimeout());
        return resolver.resolvePolicyEnforcer(routingContext, routingContext.get(OidcTenantConfig.class.getName()))
                .flatMap(new Function<PolicyEnforcer, Uni<? extends AuthorizationContext>>() {
                    @Override
                    public Uni<AuthorizationContext> apply(PolicyEnforcer policyEnforcer) {
                        storePolicyEnforcerOnContext(policyEnforcer, routingContext);
                        return blockingExecutor.executeBlocking(new Supplier<AuthorizationContext>() {
                            @Override
                            public AuthorizationContext get() {
                                return policyEnforcer.enforce(httpFacade, httpFacade);
                            }
                        });
                    }
                }).map(new Function<AuthorizationContext, CheckResult>() {
                    @Override
                    public CheckResult apply(AuthorizationContext authorizationContext) {
                        if (authorizationContext.isGranted()) {
                            return new CheckResult(true, enhanceSecurityIdentity(identity, authorizationContext));
                        }
                        return CheckResult.DENY;
                    }
                });
    }

    private static void storePolicyEnforcerOnContext(PolicyEnforcer policyEnforcer, RoutingContext routingContext) {
        routingContext.put(POLICY_ENFORCER, policyEnforcer);
    }

    private static SecurityIdentity enhanceSecurityIdentity(SecurityIdentity current, AuthorizationContext context) {
        return QuarkusSecurityIdentity
                .builder(current)
                .addAttribute(PERMISSIONS_ATTRIBUTE, context.getPermissions())
                .addPermissionChecker(new Function<Permission, Uni<Boolean>>() {
                    @Override
                    public Uni<Boolean> apply(Permission permission) {
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
                })
                .build();
    }
}
