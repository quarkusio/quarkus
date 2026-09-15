package io.quarkus.arc.deployment;

import java.util.List;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationStartBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;

public class LifecycleEventsBuildStep {

    @BuildStep
    ApplicationStartBuildItem startupEvent(ActionBuilder action,
            List<ServiceStartBuildItem> startList,
            BeanContainerBuildItem beanContainer,
            LaunchModeBuildItem launchMode, ArcConfig config) {
        LaunchMode mode = launchMode.getLaunchMode();
        boolean disableObservers = config.test().disableApplicationLifecycleObservers();

        action
                .forService("io.quarkus.arc.startup-event")
                .after("io.quarkus.arc.executor")
                .afterBuildItem(SyntheticBeansRuntimeInitBuildItem.class)
                .afterBuildItem(ServiceStartBuildItem.class)
                .action(ctx -> {
                    List<Class<?>> mockBeanClasses = ArcRecorder.computeMockBeanClasses(mode, disableObservers);
                    ArcRecorder.fireLifecycleEvent(new StartupEvent(), mockBeanClasses);
                    ctx.onStop(() -> ArcRecorder.performShutdown(mockBeanClasses));
                });

        action
                .forService("io.quarkus.arc.shutdown-gateway")
                .atPhase(Phase.INFRASTRUCTURE)
                .after("io.quarkus.core.last-runtime-shutdown-tasks")
                .before("io.quarkus.arc.startup-event")
                .action(ctx -> {
                    ctx.onStop(Arc::shutdown);
                });

        return new ApplicationStartBuildItem();
    }

}
