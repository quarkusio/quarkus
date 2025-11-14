package io.quarkus.deployment.pkg.steps;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.quarkus.deployment.IsProduction;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceHandledBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.runtime.LaunchMode;

public class FileSystemResourcesBuildStep {

    @BuildStep(onlyIfNot = IsProduction.class)
    public void notNormalMode(OutputTargetBuildItem outputTargetBuildItem,
            LaunchModeBuildItem launchMode,
            List<GeneratedFileSystemResourceBuildItem> generatedFileSystemResources,
            BuildProducer<GeneratedFileSystemResourceHandledBuildItem> producer) {
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            write(generatedFileSystemResources, outputTargetBuildItem.getOutputDirectory());
        }
        producer.produce(new GeneratedFileSystemResourceHandledBuildItem());
    }

    @BuildStep(onlyIf = NativeImageFutureDefault.RunTimeInitializeFileSystemProvider.class)
    RuntimeInitializedPackageBuildItem runtimeInitialized() {
        return new RuntimeInitializedPackageBuildItem("io.quarkus.fs.util");
    }

    @BuildStep(onlyIf = NativeImageFutureDefault.RunTimeInitializeFileSystemProvider.class)
    ReflectiveClassBuildItem setupReflectionClasses() {
        return ReflectiveClassBuildItem.builder("jdk.nio.zipfs.ZipFileSystemProvider").build();
    }

    @BuildStep(onlyIf = IsProduction.class)
    public void normalMode(OutputTargetBuildItem outputTargetBuildItem,
            List<GeneratedFileSystemResourceBuildItem> generatedFileSystemResources,
            // this is added to ensure that the build step will be run
            BuildProducer<ArtifactResultBuildItem> dummy) {
        write(generatedFileSystemResources, outputTargetBuildItem.getOutputDirectory());
    }

    private void write(List<GeneratedFileSystemResourceBuildItem> generatedFileSystemResources, Path outputDirectory) {
        try {
            for (GeneratedFileSystemResourceBuildItem generatedFileSystemResource : generatedFileSystemResources) {
                Path outputPath = outputDirectory.resolve(generatedFileSystemResource.getName());
                Files.createDirectories(outputPath.getParent());
                try (OutputStream out = Files.newOutputStream(outputPath)) {
                    out.write(generatedFileSystemResource.getData());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
