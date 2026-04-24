package io.quarkus.deployment.steps;

import java.util.UUID;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInstanceIdBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;

public class ApplicationInstanceIdBuildStep {

    record ApplicationInstanceId(UUID uuid) {
    }

    @BuildStep
    public ApplicationInstanceIdBuildItem create(LiveReloadBuildItem liveReloadBuildItem) {
        ApplicationInstanceId instanceId;
        if (liveReloadBuildItem.isLiveReload()) {
            instanceId = liveReloadBuildItem.getContextObject(ApplicationInstanceId.class);
        } else {
            instanceId = new ApplicationInstanceId(UUID.randomUUID());
            liveReloadBuildItem.setContextObject(ApplicationInstanceId.class, instanceId);
        }
        return new ApplicationInstanceIdBuildItem(instanceId.uuid());
    }
}
