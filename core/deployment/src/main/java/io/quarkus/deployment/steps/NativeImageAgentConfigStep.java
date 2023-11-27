package io.quarkus.deployment.steps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageAgentConfigDirectoryBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

public class NativeImageAgentConfigStep {
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void transformConfig(BuildProducer<NativeImageAgentConfigDirectoryBuildItem> nativeImageAgentConfigDirectoryProducer,
            NativeImageSourceJarBuildItem nativeImageSourceJarBuildItem,
            BuildSystemTargetBuildItem buildSystemTargetBuildItem) throws IOException {
        final Path basePath = buildSystemTargetBuildItem.getOutputDirectory()
                .resolve(Path.of("native-agent-base-config"));
        if (basePath.toFile().exists()) {
            final Path outputDir = nativeImageSourceJarBuildItem.getPath().getParent();
            final String targetDirName = "native-agent-config";
            final Path targetPath = outputDir.resolve(Path.of(targetDirName));
            if (!targetPath.toFile().exists()) {
                targetPath.toFile().mkdirs();
            }
            nativeImageAgentConfigDirectoryProducer.produce(new NativeImageAgentConfigDirectoryBuildItem(targetDirName));

            transformConfig(basePath, "jni-config.json", targetPath);
            transformConfig(basePath, "proxy-config.json", targetPath);
            transformConfig(basePath, "reflect-config.json", targetPath);
            transformConfig(basePath, "resource-config.json", targetPath);
            transformConfig(basePath, "serialization-config.json", targetPath);
        }
    }

    private void transformConfig(Path base, String name, Path target) throws IOException {
        final Path baseConfig = base.resolve(name);
        final Path targetConfig = target.resolve(name);
        if (baseConfig.toFile().exists()) {
            Files.copy(baseConfig, targetConfig);
        }
    }
}
