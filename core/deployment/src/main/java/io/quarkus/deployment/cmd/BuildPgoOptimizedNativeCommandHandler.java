package io.quarkus.deployment.cmd;

import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;

/**
 * Used by the build tool to consume the result of the PGO-optimized native image build
 */
public class BuildPgoOptimizedNativeCommandHandler implements BiConsumer<Object, BuildResult> {

    @Override
    public void accept(Object o, BuildResult buildResult) {
        // Result handling is done in the build step
    }
}
