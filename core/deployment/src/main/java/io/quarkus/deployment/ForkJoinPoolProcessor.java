package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;

public final class ForkJoinPoolProcessor {

    @BuildStep
    SystemPropertyBuildItem setProperty() {
        return new SystemPropertyBuildItem("java.util.concurrent.ForkJoinPool.common.threadFactory",
                "io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory");
    }
}
