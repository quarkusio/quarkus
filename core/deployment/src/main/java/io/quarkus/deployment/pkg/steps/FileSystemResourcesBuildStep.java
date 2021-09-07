package io.quarkus.deployment.pkg.steps;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceHandledBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

public class FileSystemResourcesBuildStep {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void developmentMode(OutputTargetBuildItem outputTargetBuildItem,
            List<GeneratedFileSystemResourceBuildItem> generatedFileSystemResources,
            BuildProducer<GeneratedFileSystemResourceHandledBuildItem> producer) {
        write(generatedFileSystemResources, outputTargetBuildItem.getOutputDirectory());
        producer.produce(new GeneratedFileSystemResourceHandledBuildItem());
    }

    @BuildStep(onlyIfNot = IsDevelopment.class)
    public void notDevelopmentMode(OutputTargetBuildItem outputTargetBuildItem,
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
