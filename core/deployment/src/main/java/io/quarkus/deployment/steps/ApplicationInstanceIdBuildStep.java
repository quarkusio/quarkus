package io.quarkus.deployment.steps;

import java.util.UUID;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInstanceIdBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;

public class ApplicationInstanceIdBuildStep {

    private static volatile UUID uuid = null;

    @BuildStep
    public ApplicationInstanceIdBuildItem create(CuratedApplicationShutdownBuildItem buildItem) {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        buildItem.addCloseTask(() -> {
            uuid = null;

        }, true);
        return new ApplicationInstanceIdBuildItem(uuid);
    }
}
