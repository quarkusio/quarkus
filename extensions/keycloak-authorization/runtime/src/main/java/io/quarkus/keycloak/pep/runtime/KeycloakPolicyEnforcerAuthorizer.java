package io.quarkus.keycloak.pep.runtime;

import java.security.Permission;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.keycloak.AuthorizationContext;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.authorization.KeycloakAdapterPolicyEnforcer;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Tls.Verification;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class KeycloakPolicyEnforcerAuthorizer
        implements HttpSecurityPolicy, BiFunction<RoutingContext, SecurityIdentity, HttpSecurityPolicy.CheckResult> {

    private volatile KeycloakAdapterPolicyEnforcer delegate;
    private volatile long readTimeout;

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return requestContext.runBlocking(request, identity, this);
    }

    @Override
    public CheckResult apply(RoutingContext routingContext, SecurityIdentity identity) {
        VertxHttpFacade httpFacade = new VertxHttpFacade(routingContext, readTimeout);
        AuthorizationContext result = delegate.authorize(httpFacade);

        if (result.isGranted()) {
            SecurityIdentity newIdentity = enhanceSecurityIdentity(identity, result);
            return new CheckResult(true, newIdentity);
        }

        return CheckResult.DENY;
    }

    private SecurityIdentity enhanceSecurityIdentity(SecurityIdentity current,
            AuthorizationContext context) {
        Map<String, Object> attributes = new HashMap<>(current.getAttributes());

        if (context != null) {
            attributes.put("permissions", context.getPermissions());
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

    public void init(OidcConfig oidcConfig, KeycloakPolicyEnforcerConfig config, TlsConfig tlsConfig,
            HttpConfiguration httpConfiguration) {
        AdapterConfig adapterConfig = new AdapterConfig();
        String authServerUrl = oidcConfig.defaultTenant.getAuthServerUrl().get();

        try {
            adapterConfig.setRealm(authServerUrl.substring(authServerUrl.lastIndexOf('/') + 1));
            adapterConfig.setAuthServerUrl(authServerUrl.substring(0, authServerUrl.lastIndexOf("/realms")));
        } catch (Exception cause) {
            throw new RuntimeException("Failed to parse the realm name.", cause);
        }

        adapterConfig.setResource(oidcConfig.defaultTenant.getClientId().get());
        adapterConfig.setCredentials(getCredentials(oidcConfig.defaultTenant));

        boolean trustAll = oidcConfig.defaultTenant.tls.getVerification().isPresent()
                ? oidcConfig.defaultTenant.tls.getVerification().get() == Verification.NONE
                : tlsConfig.trustAll;
        if (trustAll) {
            adapterConfig.setDisableTrustManager(true);
            adapterConfig.setAllowAnyHostname(true);
        }

        PolicyEnforcerConfig enforcerConfig = getPolicyEnforcerConfig(config, adapterConfig);

        if (enforcerConfig == null) {
            return;
        }

        adapterConfig.setPolicyEnforcerConfig(enforcerConfig);

        this.readTimeout = httpConfiguration.readTimeout.toMillis();
        this.delegate = new KeycloakAdapterPolicyEnforcer(
                new PolicyEnforcer(KeycloakDeploymentBuilder.build(adapterConfig), adapterConfig));
    }

    private Map<String, Object> getCredentials(OidcTenantConfig oidcConfig) {
        Map<String, Object> credentials = new HashMap<>();
        Optional<String> clientSecret = oidcConfig.getCredentials().getSecret();

        if (clientSecret.isPresent()) {
            credentials.put("secret", clientSecret.orElse(null));
        }

        return credentials;
    }

    private Map<String, Map<String, Object>> getClaimInformationPointConfig(
            KeycloakPolicyEnforcerConfig.KeycloakConfigPolicyEnforcer.ClaimInformationPointConfig config) {
        Map<String, Map<String, Object>> cipConfig = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : config.simpleConfig.entrySet()) {
            cipConfig.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        for (Map.Entry<String, Map<String, Map<String, String>>> entry : config.complexConfig.entrySet()) {
            cipConfig.computeIfAbsent(entry.getKey(), s -> new HashMap<>()).putAll(new HashMap<>(entry.getValue()));
        }

        return cipConfig;
    }

    private PolicyEnforcerConfig getPolicyEnforcerConfig(KeycloakPolicyEnforcerConfig config, AdapterConfig adapterConfig) {
        if (config.policyEnforcer != null && config.policyEnforcer.enable) {
            PolicyEnforcerConfig enforcerConfig = new PolicyEnforcerConfig();

            enforcerConfig.setLazyLoadPaths(config.policyEnforcer.lazyLoadPaths);
            enforcerConfig.setEnforcementMode(config.policyEnforcer.enforcementMode);
            enforcerConfig.setHttpMethodAsScope(config.policyEnforcer.httpMethodAsScope);

            KeycloakPolicyEnforcerConfig.KeycloakConfigPolicyEnforcer.PathCacheConfig pathCache = config.policyEnforcer.pathCache;

            PolicyEnforcerConfig.PathCacheConfig pathCacheConfig = new PolicyEnforcerConfig.PathCacheConfig();
            pathCacheConfig.setLifespan(pathCache.lifespan);
            pathCacheConfig.setMaxEntries(pathCache.maxEntries);
            enforcerConfig.setPathCacheConfig(pathCacheConfig);

            enforcerConfig.setClaimInformationPointConfig(
                    getClaimInformationPointConfig(config.policyEnforcer.claimInformationPoint));
            enforcerConfig.setPaths(config.policyEnforcer.paths.values().stream().map(
                    pathConfig -> {
                        PolicyEnforcerConfig.PathConfig config1 = new PolicyEnforcerConfig.PathConfig();

                        config1.setName(pathConfig.name.orElse(null));
                        config1.setPath(pathConfig.path.orElse(null));
                        config1.setEnforcementMode(pathConfig.enforcementMode);
                        config1.setMethods(pathConfig.methods.values().stream().map(
                                methodConfig -> {
                                    PolicyEnforcerConfig.MethodConfig mConfig = new PolicyEnforcerConfig.MethodConfig();

                                    mConfig.setMethod(methodConfig.method);
                                    mConfig.setScopes(methodConfig.scopes);
                                    mConfig.setScopesEnforcementMode(methodConfig.scopesEnforcementMode);

                                    return mConfig;
                                }).collect(Collectors.toList()));
                        config1.setClaimInformationPointConfig(
                                getClaimInformationPointConfig(pathConfig.claimInformationPoint));

                        return config1;
                    }).collect(Collectors.toList()));

            return enforcerConfig;
        }

        return null;
    }
}
