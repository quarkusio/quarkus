package io.quarkus.dockerfiles.cli;

import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.dockerfiles.spi.GeneratedJvmDockerfileBuildItem;

public class GenerateDockerfilesHandler implements BiConsumer<Object, BuildResult> {

    @Override
    public void accept(Object context, BuildResult buildResult) {
        GeneratedJvmDockerfileBuildItem result = buildResult.consume(GeneratedJvmDockerfileBuildItem.class);
        System.out.println(result.getDockerFile());
    }
}
