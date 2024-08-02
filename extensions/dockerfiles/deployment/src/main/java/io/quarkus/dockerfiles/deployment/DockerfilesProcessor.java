package io.quarkus.dockerfiles.deployment;

import java.util.Optional;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
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
            OutputTargetBuildItem outputTarget) {
        return new GeneratedDockerfile.Jvm(DockerfileContent.getJvmDockerfileContent(effective.getFrom(),
                applicationInfo.getName(), outputTarget.getOutputDirectory()));
    }

    @BuildStep
    GeneratedDockerfile.Native buildNativeDockerfile(NativeDockerfileFrom.Effective effective,
            ApplicationInfoBuildItem applicationInfo, OutputTargetBuildItem outputTarget) {
        return new GeneratedDockerfile.Native(DockerfileContent.getNativeDockerfileContent(effective.getFrom(),
                applicationInfo.getName(), outputTarget.getOutputDirectory()));
    }
}
