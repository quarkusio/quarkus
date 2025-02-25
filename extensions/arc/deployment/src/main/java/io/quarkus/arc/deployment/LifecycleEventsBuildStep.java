package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.List;

import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationStartBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;

public class LifecycleEventsBuildStep {

    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    @BuildStep
    @Record(RUNTIME_INIT)
    ApplicationStartBuildItem startupEvent(ArcRecorder recorder,
            List<ServiceStartBuildItem> startList,
            BeanContainerBuildItem beanContainer,
            ShutdownContextBuildItem shutdown,
            LaunchModeBuildItem launchMode, ArcConfig config) {
        recorder.handleLifecycleEvents(shutdown, launchMode.getLaunchMode(),
                config.test().disableApplicationLifecycleObservers());
        return new ApplicationStartBuildItem();
    }

}
