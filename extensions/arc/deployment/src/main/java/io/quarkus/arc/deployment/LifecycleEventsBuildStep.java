package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.List;

import javax.enterprise.context.BeforeDestroyed;

import io.quarkus.arc.runtime.ArcDeploymentTemplate;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

public class LifecycleEventsBuildStep {

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, BeforeDestroyed.class.getName()));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void startupEvent(ArcDeploymentTemplate template, List<ServiceStartBuildItem> startList,
            BeanContainerBuildItem beanContainer,
            ShutdownContextBuildItem shutdown) {
        template.handleLifecycleEvents(shutdown, beanContainer.getValue());
    }

}
