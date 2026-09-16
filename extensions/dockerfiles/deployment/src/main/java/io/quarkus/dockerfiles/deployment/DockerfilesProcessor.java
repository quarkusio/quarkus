package io.quarkus.dockerfiles.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.dockerfiles.spi.DockerfileDependencyBuildItem;
import io.quarkus.dockerfiles.spi.GeneratedDockerfile;
import io.quarkus.dockerfiles.spi.JvmDockerfileFrom;
import io.quarkus.dockerfiles.spi.NativeDockerfileFrom;

class DockerfilesProcessor {

    private static final String FEATURE = "dockerfiles";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    JvmDockerfileFrom.Effective effectiveJvmDockerfile(DockerfilesConfiguration config,
            Optional<JvmDockerfileFrom.Selected> selected) {
        // 1. Get FROM explicitly configured
        // 2. Get FROM using BuildItems
        // 3. Get FROM using default value
        String from = config.getConfiguredJvmFrom()
                .orElse(selected.map(JvmDockerfileFrom.Selected::getFrom).orElse(DockerfilesConfiguration.DEFAULT_JVM_FROM));
        return new JvmDockerfileFrom.Effective(from);
    }

    @BuildStep
    NativeDockerfileFrom.Effective effectiveNativeDockerfile(DockerfilesConfiguration config,
            Optional<NativeDockerfileFrom.Selected> selected) {
        // 1. Get FROM explicitly configured
        // 2. Get FROM using BuildItems
        // 3. Get FROM using default value
        String from = config.getConfiguredNativeFrom().orElse(
                selected.map(NativeDockerfileFrom.Selected::getFrom).orElse(DockerfilesConfiguration.DEFAULT_NATIVE_FROM));
        return new NativeDockerfileFrom.Effective(from);
    }

    @BuildStep
    GeneratedDockerfile.Jvm buildJvmDockerfile(JvmDockerfileFrom.Effective effective, ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget, List<DockerfileDependencyBuildItem> dependencies) {
        String content = DockerfileContent.jvmBuilder()
                .from(effective.getFrom())
                .applicationName(applicationInfo.getName())
                .outputDir(outputTarget.getOutputDirectory())
                .dependencies(dependencies)
                .build();
        return new GeneratedDockerfile.Jvm(content);
    }

    @BuildStep
    GeneratedDockerfile.Native buildNativeDockerfile(NativeDockerfileFrom.Effective effective,
            ApplicationInfoBuildItem applicationInfo, OutputTargetBuildItem outputTarget,
            List<DockerfileDependencyBuildItem> dependencies) {
        String content = DockerfileContent.nativeBuilder()
                .from(effective.getFrom())
                .applicationName(applicationInfo.getName())
                .outputDir(outputTarget.getOutputDirectory())
                .dependencies(dependencies)
                .build();
        return new GeneratedDockerfile.Native(content);
    }
}
