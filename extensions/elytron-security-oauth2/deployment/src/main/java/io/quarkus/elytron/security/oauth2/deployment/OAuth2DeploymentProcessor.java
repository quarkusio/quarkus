package io.quarkus.elytron.security.oauth2.deployment;

import java.util.function.BooleanSupplier;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elytron.security.deployment.ElytronTokenMarkerBuildItem;
import io.quarkus.elytron.security.oauth2.runtime.OAuth2BuildTimeConfig;
import io.quarkus.elytron.security.oauth2.runtime.OAuth2Recorder;
import io.quarkus.elytron.security.oauth2.runtime.OAuth2RuntimeConfig;
import io.quarkus.elytron.security.oauth2.runtime.auth.OAuth2Augmentor;
import io.quarkus.elytron.security.oauth2.runtime.auth.OAuth2AuthMechanism;

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

    @BuildStep(onlyIf = IsOAuth2Enabled.class)
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(Feature.SECURITY_OAUTH2);
    }

    @BuildStep(onlyIf = IsOAuth2Enabled.class)
    AdditionalBeanBuildItem configureOauth2RealmAuthConfig(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar.forService(SecurityRealm.class, REALM_NAME)
                .atPhase(Phase.DATA)
                .require(OAuth2RuntimeConfig.class)
                .onStart((ctx, runtimeConfig) -> OAuth2Recorder.createRealm(runtimeConfig));
        return AdditionalBeanBuildItem.unremovableOf(OAuth2AuthMechanism.class);
    }

    @BuildStep(onlyIf = IsOAuth2Enabled.class)
    ElytronTokenMarkerBuildItem marker() {
        return new ElytronTokenMarkerBuildItem();
    }

    @BuildStep(onlyIf = IsOAuth2Enabled.class)
    AdditionalBeanBuildItem registerOAuth2AugmentorAsCdiBean() {
        return AdditionalBeanBuildItem.unremovableOf(OAuth2Augmentor.class);
    }

    static final class IsOAuth2Enabled implements BooleanSupplier {

        private final boolean enabled;

        IsOAuth2Enabled(OAuth2BuildTimeConfig oauth2BuildTimeConfig) {
            this.enabled = oauth2BuildTimeConfig.enabled();
        }

        @Override
        public boolean getAsBoolean() {
            return enabled;
        }
    }
}
