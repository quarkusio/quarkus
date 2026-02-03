package io.quarkus.deployment.cmd;

import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;

/**
 * Used by the build tool to consume the result of the Quarkus augmentation with the AOT file data
 */
public class BuildEnhancedAotContainerImageCommandHandler implements BiConsumer<Object, BuildResult> {

    @Override
    public void accept(Object o, BuildResult buildResult) {

    }
}
