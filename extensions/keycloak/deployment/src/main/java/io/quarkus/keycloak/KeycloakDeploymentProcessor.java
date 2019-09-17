package io.quarkus.keycloak;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class KeycloakDeploymentProcessor {

    @BuildStep
    void configureKeycloakAdapter(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<AdditionalBeanBuildItem> beans) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.KEYCLOAK));
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.KEYCLOAK));

        // register producers for injection and configuration of keycloak components
        beans.produce(new AdditionalBeanBuildItem(KeycloakSecurityContextProducer.class));
        beans.produce(new AdditionalBeanBuildItem(QuarkusKeycloakConfigResolver.class));
    }
}
