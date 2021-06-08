package io.quarkus.keycloak.pep.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.ClaimInformationPointConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.PathCacheConfig;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Tls.Verification;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.smallrye.mutiny.Uni;

@Recorder
public class KeycloakPolicyEnforcerRecorder {

    private static final Map<String, KeycloakPolicyEnforcerContext> dynamicPoliciesConfig = new ConcurrentHashMap<>();
    private static final Logger LOG = Logger.getLogger(KeycloakPolicyEnforcerRecorder.class);

    public Supplier<KeycloakPolicyEnforcerConfigBean> setup(OidcConfig oidcConfig, KeycloakPolicyEnforcerConfig config,
            TlsConfig tlsConfig, HttpConfiguration httpConfiguration) {
        KeycloakPolicyEnforcerContext defaultPolicyEnforcerContext = oidcConfig.defaultTenant.tenantEnabled
                ? createPolicyConfigContext(oidcConfig.defaultTenant,
                        config.defaultTenant, tlsConfig,
                        httpConfiguration)
                : null;
        Map<String, KeycloakPolicyEnforcerContext> staticPolicyEnforcerTenants = new HashMap<String, KeycloakPolicyEnforcerContext>();
        for (Map.Entry<String, KeycloakPolicyEnforcerTenantConfig> enforcerTenant : config.namedTenants.entrySet()) {
            OidcTenantConfig oidcTenantConfig = oidcConfig.namedTenants.get(enforcerTenant.getKey());
            if (oidcTenantConfig == null) {
                throw new ConfigurationException(
                        "No OIDC configuration specified in the application properties for tenant " + enforcerTenant.getKey());
            }
            staticPolicyEnforcerTenants.put(enforcerTenant.getKey(),
                    createStaticTenantContext(oidcTenantConfig, enforcerTenant.getValue(), enforcerTenant.getKey(), tlsConfig,
                            httpConfiguration));
        }
        // Add tenants without explicit policy enforcer configs will be configured with default tenant
        for (Map.Entry<String, OidcTenantConfig> tenantConfig : oidcConfig.namedTenants.entrySet()) {
            if (!staticPolicyEnforcerTenants.containsKey(tenantConfig.getKey())) {
                staticPolicyEnforcerTenants.put(tenantConfig.getKey(),
                        createStaticTenantContext(tenantConfig.getValue(), config.defaultTenant,
                                tenantConfig.getKey(),
                                tlsConfig,
                                httpConfiguration));
            }
        }
        return new Supplier<KeycloakPolicyEnforcerConfigBean>() {
            @Override
            public KeycloakPolicyEnforcerConfigBean get() {
                return new KeycloakPolicyEnforcerConfigBean(oidcConfig, defaultPolicyEnforcerContext,
                        staticPolicyEnforcerTenants,
                        dynamicPoliciesConfig, httpConfiguration.readTimeout.toMillis(),
                        new BiFunction<OidcTenantConfig, KeycloakPolicyEnforcerTenantConfig, Uni<KeycloakPolicyEnforcerContext>>() {
                            @Override
                            public Uni<KeycloakPolicyEnforcerContext> apply(OidcTenantConfig oidcConfig,
                                    KeycloakPolicyEnforcerTenantConfig enforcerConfig) {
                                return createDynamicPolicyConfigContext(oidcConfig, enforcerConfig, tlsConfig,
                                        httpConfiguration)
                                                .plug(u -> {
                                                    if (!BlockingOperationControl.isBlockingAllowed()) {
                                                        return u.runSubscriptionOn(ExecutorRecorder.getCurrent());
                                                    }
                                                    return u;
                                                });
                            }
                        });
            }
        };
    }

    private static Uni<KeycloakPolicyEnforcerContext> createDynamicPolicyConfigContext(OidcTenantConfig oidcConfig,
            KeycloakPolicyEnforcerTenantConfig keycloakPolicyEnforcerConfig,
            TlsConfig tlsConfig, HttpConfiguration httpConfiguration) {
        String tenantId = oidcConfig.getTenantId().get();

        if (!dynamicPoliciesConfig.containsKey(tenantId)) {
            Uni<KeycloakPolicyEnforcerContext> uniContext = Uni.createFrom().item(
                    () -> createPolicyConfigContext(oidcConfig, keycloakPolicyEnforcerConfig, tlsConfig, httpConfiguration));
            uniContext.onFailure().transform(t -> logTenantConfigContextFailure(t, tenantId));
            return uniContext.onItem().transform(
                    new Function<KeycloakPolicyEnforcerContext, KeycloakPolicyEnforcerContext>() {
                        @Override
                        public KeycloakPolicyEnforcerContext apply(KeycloakPolicyEnforcerContext c) {
                            dynamicPoliciesConfig.putIfAbsent(tenantId, c);
                            return c;
                        }

                    });
        } else {
            return Uni.createFrom().item(dynamicPoliciesConfig.get(tenantId));
        }

    }

    private KeycloakPolicyEnforcerContext createStaticTenantContext(OidcTenantConfig oidcConfig,
            KeycloakPolicyEnforcerTenantConfig keycloakPolicyEnforcerConfig,
            String tenantId,
            TlsConfig tlsConfig, HttpConfiguration httpConfiguration) {
        Uni<KeycloakPolicyEnforcerContext> uniContext = Uni.createFrom()
                .item(() -> createPolicyConfigContext(oidcConfig, keycloakPolicyEnforcerConfig, tlsConfig, httpConfiguration));
        return uniContext.onFailure()
                .recoverWithItem(new Function<Throwable, KeycloakPolicyEnforcerContext>() {
                    @Override
                    public KeycloakPolicyEnforcerContext apply(Throwable t) {
                        if (t instanceof OIDCException) {
                            // OIDC server is not available yet - try to create the connection when the first request arrives
                            LOG.debugf("Tenant '%s': '%s'."
                                    + " Access to resources protected by this tenant may fail"
                                    + " if Keycloak Authorization Services does not become available.",
                                    tenantId, t.getMessage());
                            return new KeycloakPolicyEnforcerContext(null, oidcConfig, keycloakPolicyEnforcerConfig);
                        }
                        logTenantConfigContextFailure(t, tenantId);
                        if (t instanceof ConfigurationException
                                && !oidcConfig.authServerUrl.isPresent() && LaunchMode.DEVELOPMENT == LaunchMode.current()) {
                            // Let it start if it is a DEV mode and auth-server-url has not been configured yet
                            return new KeycloakPolicyEnforcerContext(null, oidcConfig, keycloakPolicyEnforcerConfig,
                                    false);
                        }
                        // fail in all other cases
                        throw new OIDCException(t);
                    }
                })
                .await().indefinitely();
    }

    private static KeycloakPolicyEnforcerContext createPolicyConfigContext(OidcTenantConfig oidcConfig,
            KeycloakPolicyEnforcerTenantConfig keycloakPolicyEnforcerConfig,
            TlsConfig tlsConfig, HttpConfiguration httpConfiguration) {
        PolicyEnforcer pe = createPolicyEnforcer(oidcConfig, keycloakPolicyEnforcerConfig, tlsConfig, httpConfiguration);

        return new KeycloakPolicyEnforcerContext(pe, oidcConfig, keycloakPolicyEnforcerConfig);

    }

    private static PolicyEnforcer createPolicyEnforcer(OidcTenantConfig oidcConfig,
            KeycloakPolicyEnforcerTenantConfig keycloakPolicyEnforcerConfig,
            TlsConfig tlsConfig, HttpConfiguration httpConfiguration) {

        if (oidcConfig.applicationType == OidcTenantConfig.ApplicationType.WEB_APP
                && oidcConfig.roles.source.orElse(null) != Source.accesstoken) {
            throw new OIDCException("Application 'web-app' type is only supported if access token is the source of roles");
        }

        AdapterConfig adapterConfig = new AdapterConfig();
        String authServerUrl = oidcConfig.getAuthServerUrl().get();

        try {
            adapterConfig.setRealm(authServerUrl.substring(authServerUrl.lastIndexOf('/') + 1));
            adapterConfig.setAuthServerUrl(authServerUrl.substring(0, authServerUrl.lastIndexOf("/realms")));
        } catch (Exception cause) {
            throw new ConfigurationException("Failed to parse the realm name.", cause);
        }

        adapterConfig.setResource(oidcConfig.getClientId().get());
        adapterConfig.setCredentials(getCredentials(oidcConfig));

        boolean trustAll = oidcConfig.tls.getVerification().isPresent()
                ? oidcConfig.tls.getVerification().get() == Verification.NONE
                : tlsConfig.trustAll;
        if (trustAll) {
            adapterConfig.setDisableTrustManager(true);
            adapterConfig.setAllowAnyHostname(true);
        }
        adapterConfig.setConnectionPoolSize(keycloakPolicyEnforcerConfig.connectionPoolSize);

        if (oidcConfig.proxy.host.isPresent()) {
            adapterConfig.setProxyUrl(oidcConfig.proxy.host.get() + ":"
                    + oidcConfig.proxy.port);
        }

        PolicyEnforcerConfig enforcerConfig = getPolicyEnforcerConfig(keycloakPolicyEnforcerConfig,
                adapterConfig);

        adapterConfig.setPolicyEnforcerConfig(enforcerConfig);

        return new PolicyEnforcer(KeycloakDeploymentBuilder.build(adapterConfig), adapterConfig);
    }

    private static Map<String, Object> getCredentials(OidcTenantConfig oidcConfig) {
        Map<String, Object> credentials = new HashMap<>();
        Optional<String> clientSecret = oidcConfig.getCredentials().getSecret();

        if (clientSecret.isPresent()) {
            credentials.put("secret", clientSecret.orElse(null));
        }

        return credentials;
    }

    private static Map<String, Map<String, Object>> getClaimInformationPointConfig(ClaimInformationPointConfig config) {
        Map<String, Map<String, Object>> cipConfig = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : config.simpleConfig.entrySet()) {
            cipConfig.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        for (Map.Entry<String, Map<String, Map<String, String>>> entry : config.complexConfig.entrySet()) {
            cipConfig.computeIfAbsent(entry.getKey(), s -> new HashMap<>()).putAll(new HashMap<>(entry.getValue()));
        }

        return cipConfig;
    }

    private static PolicyEnforcerConfig getPolicyEnforcerConfig(KeycloakPolicyEnforcerTenantConfig config,
            AdapterConfig adapterConfig) {
        PolicyEnforcerConfig enforcerConfig = new PolicyEnforcerConfig();

        enforcerConfig.setLazyLoadPaths(config.policyEnforcer.lazyLoadPaths);
        enforcerConfig.setEnforcementMode(config.policyEnforcer.enforcementMode);
        enforcerConfig.setHttpMethodAsScope(config.policyEnforcer.httpMethodAsScope);

        PathCacheConfig pathCache = config.policyEnforcer.pathCache;

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

    private static Throwable logTenantConfigContextFailure(Throwable t, String tenantId) {
        LOG.debugf(
                "'%s' Policy enforcer for tenant: '%s' not initialized. Access to resources protected by this tenant will fail.",
                tenantId, t.getMessage());
        return t;
    }
}
