package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.List;

import io.quarkus.arc.runtime.ArcDeploymentRecorder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;

public class LifecycleEventsBuildStep {

    @BuildStep
    @Record(RUNTIME_INIT)
    void startupEvent(ArcDeploymentRecorder template, List<ServiceStartBuildItem> startList,
            BeanContainerBuildItem beanContainer,
            ShutdownContextBuildItem shutdown) {
        template.handleLifecycleEvents(shutdown, beanContainer.getValue());
    }

}
