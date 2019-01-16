package org.jboss.shamrock.arc.deployment;

import static org.jboss.shamrock.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.List;

import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.arc.runtime.ArcDeploymentTemplate;
import org.jboss.shamrock.deployment.builditem.ServiceStartBuildItem;
import org.jboss.shamrock.deployment.builditem.ShutdownContextBuildItem;

public class LifecycleEventsBuildStep {
    
    @BuildStep
    @Record(RUNTIME_INIT)
    void startupEvent(ArcDeploymentTemplate template, List<ServiceStartBuildItem> startList, BeanContainerBuildItem beanContainer,
            ShutdownContextBuildItem shutdown) {
        template.handleLifecycleEvents(shutdown, beanContainer.getValue());
    }

}
