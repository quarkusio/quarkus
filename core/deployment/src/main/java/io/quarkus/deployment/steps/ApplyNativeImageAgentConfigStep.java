package io.quarkus.deployment.steps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageAgentConfigDirectoryBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

/**
 * This configuration step looks for native configuration folder generated
 * with the native image agent running inside Quarkus integration tests.
 * If the folder is detected and {@link NativeConfig#agentConfigurationApply()} is enabled,
 * the folder's path is passed onto the {@link io.quarkus.deployment.pkg.steps.NativeImageBuildStep},
 * wrapped inside a {@link NativeImageAgentConfigDirectoryBuildItem},
 * so that the folder is added as a configuration folder for the native image process execution.
 */
public class ApplyNativeImageAgentConfigStep {
    private static final Logger log = Logger.getLogger(ApplyNativeImageAgentConfigStep.class);

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void transformConfig(NativeConfig nativeConfig,
            BuildProducer<NativeImageAgentConfigDirectoryBuildItem> nativeImageAgentConfigDirectoryProducer,
            NativeImageSourceJarBuildItem nativeImageSourceJarBuildItem,
            BuildSystemTargetBuildItem buildSystemTargetBuildItem) throws IOException {
        final Path basePath = buildSystemTargetBuildItem.getOutputDirectory()
                .resolve(Path.of("native-image-agent-final-config"));
        if (basePath.toFile().exists() && nativeConfig.agentConfigurationApply()) {
            final Path outputDir = nativeImageSourceJarBuildItem.getPath().getParent();
            final String targetDirName = "native-image-agent-config";
            final Path targetPath = outputDir.resolve(Path.of(targetDirName));
            if (!targetPath.toFile().exists()) {
                targetPath.toFile().mkdirs();
            }
            Files.copy(basePath.resolve("reflect-config.json"), targetPath.resolve("reflect-config.json"),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.copy(basePath.resolve("serialization-config.json"), targetPath.resolve("serialization-config.json"),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.copy(basePath.resolve("jni-config.json"), targetPath.resolve("jni-config.json"),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.copy(basePath.resolve("proxy-config.json"), targetPath.resolve("proxy-config.json"),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.copy(basePath.resolve("resource-config.json"), targetPath.resolve("resource-config.json"),
                    StandardCopyOption.REPLACE_EXISTING);

            log.info("Applying native image agent generated files to current native executable build");
            nativeImageAgentConfigDirectoryProducer.produce(new NativeImageAgentConfigDirectoryBuildItem(targetDirName));
        }
    }
}
