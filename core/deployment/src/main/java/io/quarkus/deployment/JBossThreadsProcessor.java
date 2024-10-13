package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class JBossThreadsProcessor {

    @BuildStep
    RuntimeInitializedClassBuildItem build() {
        // TODO: Remove once we move to a jboss-threads version that handles this in its native-image.properties file
        // see https://github.com/jbossas/jboss-threads/pull/200
        return new RuntimeInitializedClassBuildItem("org.jboss.threads.EnhancedQueueExecutor$RuntimeFields");
    }
}
