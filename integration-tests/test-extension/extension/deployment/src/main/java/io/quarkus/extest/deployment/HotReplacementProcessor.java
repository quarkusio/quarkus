package io.quarkus.extest.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.extest.runtime.TestHotReplacementSetup;

public class HotReplacementProcessor {
    @BuildStep
    public HotDeploymentWatchedFileBuildItem registerHotReplacementFile() {
        return new HotDeploymentWatchedFileBuildItem(TestHotReplacementSetup.HOT_REPLACEMENT_FILE, false);
    }
}
