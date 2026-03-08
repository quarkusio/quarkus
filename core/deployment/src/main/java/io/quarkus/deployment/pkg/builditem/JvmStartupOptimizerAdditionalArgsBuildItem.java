package io.quarkus.deployment.pkg.builditem;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Carries additional JVM arguments (e.g. from container image extensions like Jib)
 * that should be included when generating the AOT cache training command.
 * <p>
 * This ensures that JVM arguments configured for the container runtime
 * (such as {@code quarkus.jib.jvm-additional-arguments}) are also applied
 * during AOT cache generation so the resulting cache is compatible.
 */
public final class JvmStartupOptimizerAdditionalArgsBuildItem extends SimpleBuildItem {

    private final List<String> additionalArgs;

    public JvmStartupOptimizerAdditionalArgsBuildItem(List<String> additionalArgs) {
        this.additionalArgs = additionalArgs;
    }

    public List<String> getAdditionalArgs() {
        return additionalArgs;
    }
}
