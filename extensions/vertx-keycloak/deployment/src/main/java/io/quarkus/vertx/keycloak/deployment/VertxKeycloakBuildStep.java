package io.quarkus.vertx.keycloak.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.quarkus.vertx.keycloak.KeycloakConfig;
import io.quarkus.vertx.keycloak.VertxJwtPrincipalProducer;
import io.quarkus.vertx.keycloak.VertxKeycloakRecorder;
import io.quarkus.vertx.keycloak.VertxOAuth2AuthenticationMechanism;
import io.quarkus.vertx.keycloak.VertxOAuth2IdentityProvider;

public class VertxKeycloakBuildStep {

    @BuildStep
    public AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClass(VertxOAuth2AuthenticationMechanism.class)
                .addBeanClass(VertxJwtPrincipalProducer.class)
                .addBeanClass(VertxOAuth2IdentityProvider.class).build();
    }

    @BuildStep
    EnableAllSecurityServicesBuildItem security() {
        return new EnableAllSecurityServicesBuildItem();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void setup(KeycloakConfig config, VertxKeycloakRecorder recorder, VertxBuildItem vertxBuildItem,
            BeanContainerBuildItem bc) {
        recorder.setup(config, vertxBuildItem.getVertx(), bc.getValue());
    }
}
