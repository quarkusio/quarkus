package io.quarkus.arc.deployment.init;

import java.util.List;

import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.builditem.InitTaskCompletedBuildItem;
import io.quarkus.runtime.init.InitRuntimeConfig;
import io.quarkus.runtime.init.InitializationTaskRecorder;

/**
 * A processor that is used to track all {@link io.quarkus.deployment.builditem.InitTaskCompletedBuildItem} in order to exit
 * once they are completed if
 * needed.
 */
public class InitializationTaskProcessor {

    @BuildStep
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    void startApplicationInitializer(ActionBuilder action,
            List<InitTaskCompletedBuildItem> initTaskCompletedBuildItems) {
        action
                .forService("io.quarkus.arc.init-task")
                .atPhase(Phase.INIT)
                .require(InitRuntimeConfig.class)
                .afterBuildItem(SyntheticBeansRuntimeInitBuildItem.class)
                .action((ctx, config) -> InitializationTaskRecorder.exitIfNeeded(config));
    }
}
