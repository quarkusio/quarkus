package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class JBossThreadsProcessor {

    @BuildStep
    RuntimeInitializedClassBuildItem build() {
        // TODO: Remove once we move to a jboss-threads version that handles this in its native-image.properties file
        // see https://github.com/jbossas/jboss-threads/pull/200
        return new RuntimeInitializedClassBuildItem("org.jboss.threads.EnhancedQueueExecutor$RuntimeFields");
    }

    @BuildStep
    ModuleOpenBuildItem allowClearThreadLocals() {
        // Since JDK 24, JBoss Threads needs `--add-opens java.base/java.lang=org.jboss.threads` to handle org.jboss.JDKSpecific.ThreadAccess.clearThreadLocals()
        return new ModuleOpenBuildItem("java.base", ModuleOpenBuildItem.ALL_UNNAMED, "java.lang");
    }

}
