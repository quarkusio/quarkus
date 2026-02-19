package io.quarkus.dockerfiles.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.dockerfiles.spi.GeneratedDockerfile;
import io.quarkus.dockerfiles.spi.GeneratedDockerfile.Jvm;
import io.quarkus.dockerfiles.spi.GeneratedDockerfile.Native;

public class GenerateDockerfilesHandler implements BiConsumer<Object, BuildResult> {

    @Override
    public void accept(Object context, BuildResult buildResult) {
        List<GeneratedDockerfile> dockerfiles = new ArrayList<>();

        GeneratedDockerfile.Jvm jvmDockerfile = buildResult.consumeOptional(GeneratedDockerfile.Jvm.class);
        GeneratedDockerfile.Native nativeDockerfile = buildResult.consumeOptional(GeneratedDockerfile.Native.class);

        if (jvmDockerfile != null) {
            dockerfiles.add(jvmDockerfile);
        }

        if (nativeDockerfile != null) {
            dockerfiles.add(nativeDockerfile);
        }
        Consumer<List<GeneratedDockerfile>> consumer = (Consumer<List<GeneratedDockerfile>>) context;
        consumer.accept(dockerfiles);
    }
}
