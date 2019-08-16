package io.quarkus.keycloak;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.elytron.security.deployment.AuthConfigBuildItem;
import io.quarkus.elytron.security.runtime.AuthConfig;
import io.quarkus.undertow.deployment.ServletExtensionBuildItem;

public class KeycloakAdapterProcessor {

    KeycloakConfig keycloakConfig;

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    BeanContainerListenerBuildItem configureAdapter(KeycloakRecorder recorder, BuildProducer<AuthConfigBuildItem> authConfig,
            BuildProducer<HotDeploymentWatchedFileBuildItem> resources,
            BuildProducer<ServletExtensionBuildItem> servletExtension) {
        // configure login info
        authConfig.produce(new AuthConfigBuildItem(new AuthConfig("KEYCLOAK", "KEYCLOAK", Object.class)));

        // in case keycloak.json is used, register it as a hot deployment config file
        resources.produce(new HotDeploymentWatchedFileBuildItem("keycloak.json"));

        AdapterConfig adapterConfig = null;

        // check if the adapter config is set in quarkus config and create the adapter configuration
        if (keycloakConfig.resource.isPresent()) {
            adapterConfig = createAdapterConfig(keycloakConfig);
        }

        QuarkusDeploymentContext deploymentContext = recorder.createKeycloakDeploymentContext(adapterConfig);

        // register keycloak servlet extension
        servletExtension.produce(new ServletExtensionBuildItem(recorder.createServletExtension(deploymentContext)));

        return new BeanContainerListenerBuildItem(recorder.createBeanContainerListener(deploymentContext));
    }

    private AdapterConfig createAdapterConfig(KeycloakConfig keycloakConfig) {
        AdapterConfig config = new AdapterConfig();

        config.setRealm(keycloakConfig.realm);
        config.setRealmKey(keycloakConfig.realmPublicKey.orElse(null));
        config.setAuthServerUrl(keycloakConfig.authServerUrl);
        config.setSslRequired(keycloakConfig.sslRequired);
        config.setConfidentialPort(keycloakConfig.confidentialPort);
        config.setResource(keycloakConfig.resource.get());
        config.setUseResourceRoleMappings(keycloakConfig.useResourceRoleMappings);
        config.setCors(keycloakConfig.enableCors);
        config.setCorsMaxAge(keycloakConfig.corsMaxAge);
        config.setCorsAllowedHeaders(keycloakConfig.corsAllowedHeaders.orElse(null));
        config.setCorsAllowedMethods(keycloakConfig.corsAllowedMethods.orElse(null));
        config.setCorsExposedHeaders(keycloakConfig.corsExposedHeaders.orElse(null));
        config.setBearerOnly(keycloakConfig.bearerOnly);
        config.setAutodetectBearerOnly(keycloakConfig.autodetectBearerOnly);
        config.setPublicClient(keycloakConfig.publicClient);

        Map<String, Object> credentials = new HashMap<>();

        if (keycloakConfig.credentials != null) {
            if (keycloakConfig.credentials.secret.isPresent()) {
                credentials.put("secret", keycloakConfig.credentials.secret.get());
            } else if (keycloakConfig.credentials.jwt != null && !keycloakConfig.credentials.jwt.isEmpty()) {
                Map<Object, Object> jwt = new HashMap<>();
                jwt.putAll(keycloakConfig.credentials.jwt);
                credentials.put("jwt", jwt);
            } else if (keycloakConfig.credentials.secretJwt != null && !keycloakConfig.credentials.secretJwt.isEmpty()) {
                Map<Object, Object> secretJwt = new HashMap<>();
                secretJwt.putAll(keycloakConfig.credentials.secretJwt);
                credentials.put("secret-jwt", secretJwt);
            }
        }

        config.setCredentials(credentials);

        config.setRedirectRewriteRules(keycloakConfig.redirectRewriteRules);
        config.setAllowAnyHostname(keycloakConfig.allowAnyHostname);
        config.setDisableTrustManager(keycloakConfig.disableTrustManager);
        config.setTruststore(keycloakConfig.truststore.orElse(null));
        config.setTruststorePassword(keycloakConfig.truststorePassword);
        config.setClientKeystore(keycloakConfig.clientKeystore.orElse(null));
        config.setClientKeystorePassword(keycloakConfig.clientKeystorePassword);
        config.setClientKeyPassword(keycloakConfig.clientKeyPassword);
        config.setConnectionPoolSize(keycloakConfig.connectionPoolSize);
        config.setAlwaysRefreshToken(keycloakConfig.alwaysRefreshToken);
        config.setRegisterNodeAtStartup(keycloakConfig.registerNodeAtStartup);
        config.setRegisterNodePeriod(keycloakConfig.registerNodePeriod);
        config.setTokenStore(keycloakConfig.tokenStore.orElse(null));
        config.setTokenCookiePath(keycloakConfig.adapterStateCookiePath.orElse(null));
        config.setPrincipalAttribute(keycloakConfig.principalAttribute);
        config.setTurnOffChangeSessionIdOnLogin(keycloakConfig.turnOffChangeSessionIdOnLogin);
        config.setTokenMinimumTimeToLive(keycloakConfig.tokenMinimumTimeToLive);
        config.setMinTimeBetweenJwksRequests(keycloakConfig.minTimeBetweenJwksRequests);
        config.setPublicKeyCacheTtl(keycloakConfig.publicKeyCacheTtl);
        config.setProxyUrl(keycloakConfig.proxyUrl.orElse(null));
        config.setVerifyTokenAudience(keycloakConfig.verifyTokenAudience);
        config.setIgnoreOAuthQueryParameter(keycloakConfig.ignoreOAuthQueryParameter);

        if (keycloakConfig.policyEnforcer != null && keycloakConfig.policyEnforcer.enable) {
            PolicyEnforcerConfig enforcerConfig = new PolicyEnforcerConfig();

            enforcerConfig.setLazyLoadPaths(keycloakConfig.policyEnforcer.lazyLoadPaths);
            enforcerConfig.setEnforcementMode(
                    PolicyEnforcerConfig.EnforcementMode.valueOf(keycloakConfig.policyEnforcer.enforcementMode));
            enforcerConfig.setHttpMethodAsScope(keycloakConfig.policyEnforcer.httpMethodAsScope);
            enforcerConfig.setOnDenyRedirectTo(keycloakConfig.policyEnforcer.onDenyRedirectTo.orElse(null));

            PolicyEnforcerConfig.PathCacheConfig pathCacheConfig = new PolicyEnforcerConfig.PathCacheConfig();

            pathCacheConfig.setLifespan(keycloakConfig.policyEnforcer.pathCache.lifespan);
            pathCacheConfig.setMaxEntries(keycloakConfig.policyEnforcer.pathCache.maxEntries);

            enforcerConfig.setPathCacheConfig(pathCacheConfig);

            if (keycloakConfig.policyEnforcer.userManagedAccess) {
                enforcerConfig.setUserManagedAccess(new PolicyEnforcerConfig.UserManagedAccessConfig());
            }

            enforcerConfig.setClaimInformationPointConfig(
                    getClaimInformationPointConfig(keycloakConfig.policyEnforcer.claimInformationPoint));
            enforcerConfig.setPaths(keycloakConfig.policyEnforcer.paths.values().stream().map(
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

            config.setPolicyEnforcerConfig(enforcerConfig);
        }

        return config;
    }

    private Map<String, Map<String, Object>> getClaimInformationPointConfig(
            KeycloakConfig.KeycloakConfigPolicyEnforcer.ClaimInformationPointConfig config) {
        Map<String, Map<String, Object>> cipConfig = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : config.simpleConfig.entrySet()) {
            cipConfig.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        for (Map.Entry<String, Map<String, Map<String, String>>> entry : config.complexConfig.entrySet()) {
            cipConfig.computeIfAbsent(entry.getKey(), s -> new HashMap<>()).putAll(new HashMap<>(entry.getValue()));
        }

        return cipConfig;
    }
}
