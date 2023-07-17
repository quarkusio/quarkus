package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.pkg.steps.NativeImageBuildRunner;

/**
 * The resolved factory for the native image runner.
 * <p>
 * Warning: This build item should not be consumed without the use of
 * {@link io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild} in the {@code onlyIf} method of
 * {@link io.quarkus.builder.BuildStep} because that leads to Quarkus having to resolve the container image runtime
 * unnecessarily.
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
