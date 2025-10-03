package io.quarkus.keycloak.admin.client.common.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class KeycloakAdminClientProcessor {

    @BuildStep
    public void nativeImage(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit) {
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("org.keycloak.common.util.SecretGenerator"));
    }
}
