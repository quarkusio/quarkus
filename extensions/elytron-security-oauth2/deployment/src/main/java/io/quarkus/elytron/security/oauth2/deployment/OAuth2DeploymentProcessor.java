package io.quarkus.elytron.security.oauth2.deployment;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elytron.security.deployment.AuthConfigBuildItem;
import io.quarkus.elytron.security.deployment.IdentityManagerBuildItem;
import io.quarkus.elytron.security.deployment.SecurityDomainBuildItem;
import io.quarkus.elytron.security.deployment.SecurityRealmBuildItem;
import io.quarkus.elytron.security.oauth2.runtime.OAuth2Config;
import io.quarkus.elytron.security.oauth2.runtime.OAuth2Recorder;
import io.quarkus.elytron.security.runtime.AuthConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.undertow.deployment.ServletExtensionBuildItem;
import io.undertow.security.idm.IdentityManager;
import io.undertow.servlet.ServletExtension;

/**
 * The build time process for the OAUth2 security aspects of the deployment. This creates {@linkplain BuildStep}s for
 * integration
 * with the Elytron OAUth2 security services. This supports the Elytron OAuth2
 * {@linkplain org.wildfly.security.auth.realm.token.TokenSecurityRealm} realm implementations.
 */
class OAuth2DeploymentProcessor {
    private static final String REALM_NAME = "OAuth2";
    private static final String AUTH_MECHANISM = "BEARER_TOKEN";

    OAuth2Config oauth2;

    @BuildStep(providesCapabilities = "io.quarkus.elytron.security.oauth2")
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SECURITY_OAUTH2);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.SECURITY_OAUTH2);
    }

    /**
     * Configure a TokenSecurityRealm if enabled
     *
     * @param recorder - runtime OAuth2 security recorder
     * @param securityRealm - the producer factory for the SecurityRealmBuildItem
     * @return the AuthConfigBuildItem for the realm authentication mechanism if there was an enabled PropertiesRealmConfig,
     *         null otherwise
     * @throws Exception - on any failure
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    AuthConfigBuildItem configureOauth2RealmAuthConfig(OAuth2Recorder recorder,
            BuildProducer<SecurityRealmBuildItem> securityRealm) throws Exception {
        if (oauth2.enabled) {
            RuntimeValue<SecurityRealm> realm = recorder.createRealm(oauth2);

            AuthConfig authConfig = new AuthConfig();
            authConfig.setAuthMechanism(AUTH_MECHANISM);
            authConfig.setRealmName(REALM_NAME);
            securityRealm.produce(new SecurityRealmBuildItem(realm, authConfig));
            return new AuthConfigBuildItem(authConfig);
        }
        return null;
    }

    /**
     * Create the OAuthZIdentityManager
     *
     * @param recorder - runtime recorder
     * @param securityDomain - configured SecurityDomain
     * @param identityManagerProducer - producer factory for IdentityManagerBuildItem
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureIdentityManager(OAuth2Recorder recorder, SecurityDomainBuildItem securityDomain,
            BuildProducer<IdentityManagerBuildItem> identityManagerProducer) {
        if (oauth2.enabled) {
            IdentityManager identityManager = recorder.createIdentityManager(securityDomain.getSecurityDomain(), oauth2);
            identityManagerProducer.produce(new IdentityManagerBuildItem(identityManager));
        }
    }

    /**
     * Register the Oauth2 authentication servlet extension
     *
     * @param recorder - Oauth2 runtime recorder
     * @param container - the BeanContainer for creating CDI beans
     * @return servlet extension build item
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    ServletExtensionBuildItem registerAuthExtension(OAuth2Recorder recorder, BeanContainerBuildItem container) {
        ServletExtension authExt = recorder.createAuthExtension(AUTH_MECHANISM, container.getValue());
        return new ServletExtensionBuildItem(authExt);
    }

}
