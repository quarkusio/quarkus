package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.pkg.steps.NativeImageBuildRunner;

/**
 * The resolved factory for the native image runner.
 */
public final class NativeImageRunnerBuildItem extends SimpleBuildItem {
    private final NativeImageBuildRunner buildRunner;

    public NativeImageRunnerBuildItem(NativeImageBuildRunner buildRunner) {
        this.buildRunner = buildRunner;
    }

    public NativeImageBuildRunner getBuildRunner() {
        return buildRunner;
    }

    public boolean isContainerBuild() {
        return buildRunner.isContainer();
    }
}
