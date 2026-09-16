package io.quarkus.deployment.steps;

import java.util.UUID;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInstanceIdBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationContextBuildItem;

public class ApplicationInstanceIdBuildStep {

    record ApplicationInstanceId(UUID uuid) {
    }

    @BuildStep
    public ApplicationInstanceIdBuildItem create(CuratedApplicationContextBuildItem curatedApplicationContextBuildItem) {
        ApplicationInstanceId instanceId = curatedApplicationContextBuildItem
                .getContextObject(ApplicationInstanceId.class);
        if (instanceId == null) {
            instanceId = new ApplicationInstanceId(UUID.randomUUID());
            curatedApplicationContextBuildItem.setContextObject(ApplicationInstanceId.class, instanceId);
        }
        return new ApplicationInstanceIdBuildItem(instanceId.uuid());
    }
}
