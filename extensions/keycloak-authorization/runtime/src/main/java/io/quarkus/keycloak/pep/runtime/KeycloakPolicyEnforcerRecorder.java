package io.quarkus.keycloak.pep.runtime;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.ClaimInformationPointConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.PathCacheConfig;
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
    final HttpConfiguration httpConfiguration;

    public KeycloakPolicyEnforcerRecorder(HttpConfiguration httpConfiguration) {
        this.httpConfiguration = httpConfiguration;
    }

    public Supplier<PolicyEnforcerResolver> setup(OidcConfig oidcConfig, KeycloakPolicyEnforcerConfig config,
            TlsConfig tlsConfig) {
        PolicyEnforcer defaultPolicyEnforcer = null;
        try {
            defaultPolicyEnforcer = createPolicyEnforcer(oidcConfig.defaultTenant.authServerUrl.get(),
                    oidcConfig.defaultTenant, config.defaultTenant, tlsConfig);
        } catch (Throwable t) {
            String clientAuthServerUrl = ConfigProvider.getConfig().getValue("client.quarkus.oidc.auth-server-url",
                    String.class);
            try {
                defaultPolicyEnforcer = createPolicyEnforcer(
                        clientAuthServerUrl, oidcConfig.defaultTenant, config.defaultTenant, tlsConfig);
            } catch (Throwable t2) {
                throw new RuntimeException(
                        "PolicyEnforcer first exception: \"" + errorMessage(t, oidcConfig.defaultTenant.authServerUrl.get())
                                + "\", second exception: \"" + errorMessage(t2, clientAuthServerUrl) + "\"");
            }
        }
        PolicyEnforcer theDefaultPolicyEnforcer = defaultPolicyEnforcer;
        Map<String, PolicyEnforcer> policyEnforcerTenants = new HashMap<String, PolicyEnforcer>();
        for (Map.Entry<String, KeycloakPolicyEnforcerTenantConfig> tenant : config.namedTenants.entrySet()) {
            OidcTenantConfig oidcTenantConfig = oidcConfig.namedTenants.get(tenant.getKey());
            if (oidcTenantConfig == null) {
                throw new ConfigurationException("Failed to find a matching OidcTenantConfig for tenant: " + tenant.getKey());
            }
            policyEnforcerTenants.put(tenant.getKey(),
                    createPolicyEnforcer(null, oidcTenantConfig, tenant.getValue(), tlsConfig));
        }
        return new Supplier<PolicyEnforcerResolver>() {
            @Override
            public PolicyEnforcerResolver get() {
                return new PolicyEnforcerResolver(theDefaultPolicyEnforcer, policyEnforcerTenants,
                        httpConfiguration.readTimeout.toMillis());
            }
        };
    }

    private static String errorMessage(Throwable e, String address) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return e.getMessage() + ", address:" + address + ". stacktrace:" + sw.toString();

    }

    private static PolicyEnforcer createPolicyEnforcer(String address, OidcTenantConfig oidcConfig,
            KeycloakPolicyEnforcerTenantConfig keycloakPolicyEnforcerConfig,
            TlsConfig tlsConfig) {

        if (oidcConfig.applicationType.orElse(ApplicationType.SERVICE) == OidcTenantConfig.ApplicationType.WEB_APP
                && oidcConfig.roles.source.orElse(null) != Source.accesstoken) {
            throw new OIDCException("Application 'web-app' type is only supported if access token is the source of roles");
        }

        AdapterConfig adapterConfig = new AdapterConfig();
        String authServerUrl = address;//oidcConfig.getAuthServerUrl().get();

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
}
