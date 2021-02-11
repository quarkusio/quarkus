package io.quarkus.deployment.pkg.steps;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceHandledBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.runtime.LaunchMode;

public class FileSystemResourcesBuildStep {

    @BuildStep(onlyIfNot = IsNormal.class)
    public void notNormalMode(OutputTargetBuildItem outputTargetBuildItem,
            LaunchModeBuildItem launchMode,
            List<GeneratedFileSystemResourceBuildItem> generatedFileSystemResources,
            BuildProducer<GeneratedFileSystemResourceHandledBuildItem> producer) {
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            write(generatedFileSystemResources, outputTargetBuildItem.getOutputDirectory());
        }
        producer.produce(new GeneratedFileSystemResourceHandledBuildItem());
    }

    @BuildStep(onlyIf = IsNormal.class)
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
