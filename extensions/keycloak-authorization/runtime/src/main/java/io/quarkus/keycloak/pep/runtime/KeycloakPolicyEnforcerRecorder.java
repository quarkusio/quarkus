package io.quarkus.keycloak.pep.runtime;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.ClaimInformationPointConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.MethodConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.PathCacheConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.PathConfig;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Tls.Verification;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

@Recorder
public class KeycloakPolicyEnforcerRecorder {

    public BooleanSupplier createBodyHandlerRequiredEvaluator(KeycloakPolicyEnforcerConfig config) {
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                if (isBodyHandlerRequired(config.defaultTenant())) {
                    return true;
                }
                for (KeycloakPolicyEnforcerTenantConfig tenantConfig : config.namedTenants().values()) {
                    if (isBodyHandlerRequired(tenantConfig)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public Supplier<PolicyEnforcerResolver> setup(OidcConfig oidcConfig, KeycloakPolicyEnforcerConfig config,
            TlsConfig tlsConfig, HttpConfiguration httpConfiguration) {
        PolicyEnforcer defaultPolicyEnforcer = createPolicyEnforcer(oidcConfig.defaultTenant, config.defaultTenant(),
                tlsConfig);
        Map<String, PolicyEnforcer> policyEnforcerTenants = new HashMap<String, PolicyEnforcer>();
        for (Map.Entry<String, KeycloakPolicyEnforcerTenantConfig> tenant : config.namedTenants().entrySet()) {
            OidcTenantConfig oidcTenantConfig = oidcConfig.namedTenants.get(tenant.getKey());
            if (oidcTenantConfig == null) {
                throw new ConfigurationException("Failed to find a matching OidcTenantConfig for tenant: " + tenant.getKey());
            }
            policyEnforcerTenants.put(tenant.getKey(), createPolicyEnforcer(oidcTenantConfig, tenant.getValue(), tlsConfig));
        }
        return new Supplier<PolicyEnforcerResolver>() {
            @Override
            public PolicyEnforcerResolver get() {
                return new PolicyEnforcerResolver(defaultPolicyEnforcer, policyEnforcerTenants,
                        httpConfiguration.readTimeout.toMillis());
            }
        };
    }

    private static PolicyEnforcer createPolicyEnforcer(OidcTenantConfig oidcConfig,
            KeycloakPolicyEnforcerTenantConfig keycloakPolicyEnforcerConfig,
            TlsConfig tlsConfig) {

        if (oidcConfig.applicationType.orElse(ApplicationType.SERVICE) == OidcTenantConfig.ApplicationType.WEB_APP
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
        } else if (oidcConfig.tls.trustStoreFile.isPresent()) {
            adapterConfig.setTruststore(oidcConfig.tls.trustStoreFile.get().toString());
            adapterConfig.setTruststorePassword(oidcConfig.tls.trustStorePassword.orElse("password"));
            if (Verification.CERTIFICATE_VALIDATION == oidcConfig.tls.verification.orElse(Verification.REQUIRED)) {
                adapterConfig.setAllowAnyHostname(true);
            }
        }
        adapterConfig.setConnectionPoolSize(keycloakPolicyEnforcerConfig.connectionPoolSize());

        if (oidcConfig.proxy.host.isPresent()) {
            String host = oidcConfig.proxy.host.get();
            if (!host.startsWith("http://") && !host.startsWith("https://")) {
                host = URI.create(authServerUrl).getScheme() + "://" + host;
            }
            adapterConfig.setProxyUrl(host + ":" + oidcConfig.proxy.port);
        }

        PolicyEnforcerConfig enforcerConfig = getPolicyEnforcerConfig(keycloakPolicyEnforcerConfig);

        adapterConfig.setPolicyEnforcerConfig(enforcerConfig);

        return PolicyEnforcer.builder()
                .authServerUrl(adapterConfig.getAuthServerUrl())
                .realm(adapterConfig.getRealm())
                .clientId(adapterConfig.getResource())
                .credentials(adapterConfig.getCredentials())
                .bearerOnly(adapterConfig.isBearerOnly())
                .enforcerConfig(enforcerConfig)
                .httpClient(new HttpClientBuilder().build(adapterConfig))
                .build();
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

        for (Map.Entry<String, Map<String, String>> entry : config.simpleConfig().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                Map<String, Object> newConfig = new HashMap<>();
                for (Map.Entry<String, String> e : entry.getValue().entrySet()) {
                    if (isNotComplexConfigKey(e.getKey())) {
                        newConfig.put(e.getKey(), e.getValue());
                    }
                }
                if (!newConfig.isEmpty()) {
                    cipConfig.put(entry.getKey(), newConfig);
                }
            }
        }

        for (Map.Entry<String, Map<String, Map<String, String>>> entry : config.complexConfig().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                Map<String, Object> newConfig = new HashMap<>();
                for (Map.Entry<String, Map<String, String>> e : entry.getValue().entrySet()) {
                    if (e.getValue() != null && !e.getValue().isEmpty()) {
                        // value can be empty when this key comes from the simple config
                        // see https://github.com/quarkusio/quarkus/issues/39315#issuecomment-1991604044
                        newConfig.put(e.getKey(), e.getValue());
                    }
                }
                if (!newConfig.isEmpty()) {
                    cipConfig.computeIfAbsent(entry.getKey(), s -> new HashMap<>()).putAll(newConfig);
                }
            }
        }

        return cipConfig;
    }

    private static PolicyEnforcerConfig getPolicyEnforcerConfig(KeycloakPolicyEnforcerTenantConfig config) {
        PolicyEnforcerConfig enforcerConfig = new PolicyEnforcerConfig();

        enforcerConfig.setLazyLoadPaths(config.policyEnforcer().lazyLoadPaths());
        enforcerConfig.setEnforcementMode(config.policyEnforcer().enforcementMode());
        enforcerConfig.setHttpMethodAsScope(config.policyEnforcer().httpMethodAsScope());

        PathCacheConfig pathCache = config.policyEnforcer().pathCache();

        PolicyEnforcerConfig.PathCacheConfig pathCacheConfig = new PolicyEnforcerConfig.PathCacheConfig();
        pathCacheConfig.setLifespan(pathCache.lifespan());
        pathCacheConfig.setMaxEntries(pathCache.maxEntries());
        enforcerConfig.setPathCacheConfig(pathCacheConfig);

        enforcerConfig.setClaimInformationPointConfig(
                getClaimInformationPointConfig(config.policyEnforcer().claimInformationPoint()));
        enforcerConfig.setPaths(config.policyEnforcer().paths().values().stream().flatMap(
                new Function<PathConfig, Stream<? extends PolicyEnforcerConfig.PathConfig>>() {
                    @Override
                    public Stream<? extends PolicyEnforcerConfig.PathConfig> apply(PathConfig pathConfig) {
                        var paths = getPathConfigPaths(pathConfig);
                        if (paths.isEmpty()) {
                            return Stream.of(createKeycloakPathConfig(pathConfig, null));
                        } else {
                            return paths.stream().map(new Function<String, PolicyEnforcerConfig.PathConfig>() {
                                @Override
                                public PolicyEnforcerConfig.PathConfig apply(String path) {
                                    return createKeycloakPathConfig(pathConfig, path);
                                }
                            });
                        }
                    }
                }).collect(Collectors.toList()));

        return enforcerConfig;
    }

    private static Set<String> getPathConfigPaths(PathConfig pathConfig) {
        Set<String> paths = new HashSet<>();
        if (pathConfig.path().isPresent()) {
            paths.add(pathConfig.path().get());
        }
        if (pathConfig.paths().isPresent()) {
            paths.addAll(pathConfig.paths().get());
        }
        return paths;
    }

    private static PolicyEnforcerConfig.PathConfig createKeycloakPathConfig(PathConfig pathConfig, String path) {
        PolicyEnforcerConfig.PathConfig config1 = new PolicyEnforcerConfig.PathConfig();

        config1.setName(pathConfig.name().orElse(null));
        config1.setPath(path);
        config1.setEnforcementMode(pathConfig.enforcementMode());
        config1.setMethods(pathConfig.methods().values().stream().map(
                new Function<MethodConfig, PolicyEnforcerConfig.MethodConfig>() {
                    @Override
                    public PolicyEnforcerConfig.MethodConfig apply(MethodConfig methodConfig) {
                        PolicyEnforcerConfig.MethodConfig mConfig = new PolicyEnforcerConfig.MethodConfig();

                        mConfig.setMethod(methodConfig.method());
                        mConfig.setScopes(methodConfig.scopes());
                        mConfig.setScopesEnforcementMode(methodConfig.scopesEnforcementMode());

                        return mConfig;
                    }
                }).collect(Collectors.toList()));
        config1.setClaimInformationPointConfig(
                getClaimInformationPointConfig(pathConfig.claimInformationPoint()));
        return config1;
    }

    private static boolean isBodyHandlerRequired(KeycloakPolicyEnforcerTenantConfig config) {
        if (isBodyClaimInformationPointDefined(config.policyEnforcer().claimInformationPoint().simpleConfig())) {
            return true;
        }
        for (PathConfig path : config.policyEnforcer().paths().values()) {
            if (isBodyClaimInformationPointDefined(path.claimInformationPoint().simpleConfig())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBodyClaimInformationPointDefined(Map<String, Map<String, String>> claims) {
        for (Map.Entry<String, Map<String, String>> entry : claims.entrySet()) {
            Map<String, String> value = entry.getValue();

            for (String nestedValue : value.values()) {
                if (nestedValue.contains("request.body")) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isNotComplexConfigKey(String key) {
        // ignore complexConfig keys for reasons explained in the following comment:
        // https://github.com/quarkusio/quarkus/issues/39315#issuecomment-1991604044
        return !key.contains(".");
    }
}
