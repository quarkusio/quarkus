package io.quarkus.smallrye.faulttolerance.deployment.devui;

import io.quarkus.builder.item.SimpleBuildItem;

public final class FaultToleranceInfoBuildItem extends SimpleBuildItem {
    private final int guardedMethods;

    public FaultToleranceInfoBuildItem(int guardedMethods) {
        this.guardedMethods = guardedMethods;
    }

    public int getGuardedMethods() {
        return guardedMethods;
    }
}
