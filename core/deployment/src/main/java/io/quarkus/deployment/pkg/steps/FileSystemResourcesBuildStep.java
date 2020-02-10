package io.quarkus.deployment.pkg.steps;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

public class FileSystemResourcesBuildStep {

    @BuildStep
    public void write(OutputTargetBuildItem outputTargetBuildItem,
            List<GeneratedFileSystemResourceBuildItem> generatedFileSystemResources,
            // this is added to ensure that the build step will be run
            BuildProducer<ArtifactResultBuildItem> dummy) {
        try {
            for (GeneratedFileSystemResourceBuildItem generatedFileSystemResource : generatedFileSystemResources) {
                Path outputPath = outputTargetBuildItem.getOutputDirectory().resolve(generatedFileSystemResource.getName());
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
