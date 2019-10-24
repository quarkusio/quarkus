package io.quarkus.vertx.keycloak.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.oidc.OidcConfig;
import io.quarkus.oidc.VertxJwtPrincipalProducer;
import io.quarkus.oidc.VertxKeycloakRecorder;
import io.quarkus.oidc.VertxOAuth2AuthenticationMechanism;
import io.quarkus.oidc.VertxOAuth2IdentityProvider;
import io.quarkus.vertx.deployment.VertxBuildItem;

public class VertxKeycloakBuildStep {

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(FeatureBuildItem.OIDC);
    }

    @BuildStep
    public AdditionalBeanBuildItem beans(OidcConfig config) {
        if (config.enabled) {
            return AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClass(VertxOAuth2AuthenticationMechanism.class)
                    .addBeanClass(VertxJwtPrincipalProducer.class)
                    .addBeanClass(VertxOAuth2IdentityProvider.class).build();
        }

        return null;
    }

    @BuildStep
    EnableAllSecurityServicesBuildItem security() {
        return new EnableAllSecurityServicesBuildItem();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void setup(OidcConfig config, VertxKeycloakRecorder recorder, VertxBuildItem vertxBuildItem,
            BeanContainerBuildItem bc) {
        if (config.enabled) {
            recorder.setup(config, vertxBuildItem.getVertx(), bc.getValue());
        }
    }
}
