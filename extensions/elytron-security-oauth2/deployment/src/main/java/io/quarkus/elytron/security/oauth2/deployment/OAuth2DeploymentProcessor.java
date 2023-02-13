package io.quarkus.elytron.security.oauth2.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elytron.security.deployment.ElytronTokenMarkerBuildItem;
import io.quarkus.elytron.security.deployment.SecurityRealmBuildItem;
import io.quarkus.elytron.security.oauth2.runtime.OAuth2BuildTimeConfig;
import io.quarkus.elytron.security.oauth2.runtime.OAuth2Recorder;
import io.quarkus.elytron.security.oauth2.runtime.OAuth2RuntimeConfig;
import io.quarkus.elytron.security.oauth2.runtime.auth.OAuth2AuthMechanism;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.vertx.http.deployment.SecurityInformationBuildItem;

/**
 * The build time process for the OAUth2 security aspects of the deployment. This creates {@linkplain BuildStep}s for
 * integration
 * with the Elytron OAUth2 security services. This supports the Elytron OAuth2
 * {@linkplain org.wildfly.security.auth.realm.token.TokenSecurityRealm} realm implementations.
 */
class OAuth2DeploymentProcessor {
    private static final String REALM_NAME = "OAuth2";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_OAUTH2);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(Feature.SECURITY_OAUTH2);
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
    @Record(ExecutionTime.RUNTIME_INIT)
    AdditionalBeanBuildItem configureOauth2RealmAuthConfig(OAuth2Recorder recorder,
            OAuth2BuildTimeConfig oauth2BuildTimeConfig,
            OAuth2RuntimeConfig oauth2RuntimeConfig,
            BuildProducer<SecurityRealmBuildItem> securityRealm) throws Exception {
        if (!oauth2BuildTimeConfig.enabled) {
            return null;
        }

        RuntimeValue<SecurityRealm> realm = recorder.createRealm(oauth2RuntimeConfig);
        securityRealm.produce(new SecurityRealmBuildItem(realm, REALM_NAME, null));
        return AdditionalBeanBuildItem.unremovableOf(OAuth2AuthMechanism.class);
    }

    @BuildStep
    ElytronTokenMarkerBuildItem marker(OAuth2BuildTimeConfig oauth2BuildTimeConfig) {
        if (!oauth2BuildTimeConfig.enabled) {
            return null;
        }
        return new ElytronTokenMarkerBuildItem();
    }

    void provideSecurityInformation(OAuth2BuildTimeConfig oauth2BuildTimeConfig,
            BuildProducer<SecurityInformationBuildItem> securityInformationProducer) {
        if (oauth2BuildTimeConfig.enabled) {
            securityInformationProducer.produce(SecurityInformationBuildItem.OAUTH2());
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem augmentor(OAuth2Recorder recorder,
            OAuth2BuildTimeConfig oauth2BuildTimeConfig) {
        return SyntheticBeanBuildItem.configure(SecurityIdentityAugmentor.class)
                .scope(ApplicationScoped.class)
                .runtimeValue(recorder.augmentor(oauth2BuildTimeConfig))
                .unremovable()
                .done();
    }
}
