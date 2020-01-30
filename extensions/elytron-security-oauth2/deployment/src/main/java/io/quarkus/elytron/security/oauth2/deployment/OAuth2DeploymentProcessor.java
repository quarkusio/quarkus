package io.quarkus.elytron.security.oauth2.deployment;

import javax.enterprise.context.ApplicationScoped;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elytron.security.deployment.ElytronTokenMarkerBuildItem;
import io.quarkus.elytron.security.deployment.SecurityRealmBuildItem;
import io.quarkus.elytron.security.oauth2.runtime.OAuth2Config;
import io.quarkus.elytron.security.oauth2.runtime.OAuth2Recorder;
import io.quarkus.elytron.security.oauth2.runtime.auth.OAuth2AuthMechanism;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.identity.SecurityIdentityAugmentor;

/**
 * The build time process for the OAUth2 security aspects of the deployment. This creates {@linkplain BuildStep}s for
 * integration
 * with the Elytron OAUth2 security services. This supports the Elytron OAuth2
 * {@linkplain org.wildfly.security.auth.realm.token.TokenSecurityRealm} realm implementations.
 */
class OAuth2DeploymentProcessor {
    private static final String REALM_NAME = "OAuth2";

    OAuth2Config oauth2;

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.SECURITY_ELYTRON_OAUTH2);
    }

    @BuildStep
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
    @Record(ExecutionTime.RUNTIME_INIT)
    AdditionalBeanBuildItem configureOauth2RealmAuthConfig(OAuth2Recorder recorder,
            BuildProducer<SecurityRealmBuildItem> securityRealm) throws Exception {
        if (oauth2.enabled) {
            RuntimeValue<SecurityRealm> realm = recorder.createRealm(oauth2);
            securityRealm.produce(new SecurityRealmBuildItem(realm, REALM_NAME, null));
            return AdditionalBeanBuildItem.unremovableOf(OAuth2AuthMechanism.class);
        }
        return null;
    }

    @BuildStep
    ElytronTokenMarkerBuildItem marker() {
        if (oauth2.enabled) {
            return new ElytronTokenMarkerBuildItem();
        }
        return null;
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem augmentor(OAuth2Recorder recorder) {
        return SyntheticBeanBuildItem.configure(SecurityIdentityAugmentor.class)
                .scope(ApplicationScoped.class)
                .runtimeValue(recorder.augmentor(oauth2))
                .unremovable()
                .done();
    }
}
