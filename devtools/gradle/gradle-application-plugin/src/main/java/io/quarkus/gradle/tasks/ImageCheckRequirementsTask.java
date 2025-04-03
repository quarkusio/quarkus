package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ArtifactCoords;

public abstract class ImageCheckRequirementsTask extends DefaultTask {

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    private static final String DEPLOYMENT_SUFFIX = "-deployment";
    static final String QUARKUS_PREFIX = "quarkus-";
    static final String QUARKUS_CONTAINER_IMAGE_PREFIX = "quarkus-container-image-";
    static final String QUARKUS_CONTAINER_IMAGE_BUILD = "quarkus.container-image.build";
    static final String QUARKUS_CONTAINER_IMAGE_PUSH = "quarkus.container-image.push";
    static final String QUARKUS_CONTAINER_IMAGE_BUILDER = "quarkus.container-image.builder";

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getApplicationModel();

    static final Map<String, ImageCheckRequirementsTask.Builder> BUILDERS = new HashMap<>();
    static {
        for (ImageCheckRequirementsTask.Builder builder : ImageCheckRequirementsTask.Builder.values()) {
            BUILDERS.put(builder.name(), builder);
        }
    }

    enum Builder {
        docker,
        jib,
        buildpack,
        openshift,
        podman
    }

    Optional<ImageCheckRequirementsTask.Builder> builderFromSystemProperties() {
        return Optional.ofNullable(System.getProperty(QUARKUS_CONTAINER_IMAGE_BUILDER))
                .filter(BUILDERS::containsKey)
                .map(BUILDERS::get);
    }

    List<ImageCheckRequirementsTask.Builder> availableBuilders() throws IOException {
        // This will pick up all dependencies set in the serialized ApplicationModel.
        // This means that extensions like quarkus-container-image-openshift via quarkus-openshift are not picked up
        // So, let's relax our filters a bit so that we can pickup quarkus-openshift directly (relax the prefix requirement).
        ApplicationModel appModel = ToolingUtils.deserializeAppModel(getApplicationModel().get().getAsFile().toPath());
        return appModel.getDependencies()
                .stream()
                .map(ArtifactCoords::getArtifactId)
                .filter(n -> n.startsWith(QUARKUS_CONTAINER_IMAGE_PREFIX) || n.startsWith(QUARKUS_PREFIX))
                .map(n -> n.replace(QUARKUS_CONTAINER_IMAGE_PREFIX, "").replace(QUARKUS_PREFIX, "").replace(DEPLOYMENT_SUFFIX,
                        ""))
                .filter(BUILDERS::containsKey)
                .map(BUILDERS::get)
                .collect(Collectors.toList());
    }

    private Builder builder() {
        return builderFromSystemProperties()
                .or(() -> {
                    try {
                        return availableBuilders().stream().findFirst();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(ImageCheckRequirementsTask.Builder.docker);
    }

    @TaskAction
    public void checkRequiredExtensions() throws IOException {

        // Currently forcedDependencies() is not implemented for gradle.
        // So, let's give users a meaningful warning message.
        List<Builder> availableBuidlers = availableBuilders();
        Optional<Builder> missingBuilder = Optional.of(builder()).filter(Predicate.not(availableBuidlers::contains));
        missingBuilder.map(builder -> QUARKUS_CONTAINER_IMAGE_PREFIX + builder.name()).ifPresent(missingDependency -> {
            throw new GradleException(String.format(
                    "Task: %s requires extensions: %s. " +
                            "To add the extensions to the project, you can run the following command:\n" +
                            "\tgradle addExtension --extensions=%s",
                    getName(), missingDependency, missingDependency));
        });

        if (!missingBuilder.isPresent() && availableBuidlers.isEmpty()) {
            String availableExtensions = Arrays.stream(Builder.values())
                    .map(Builder::name)
                    .collect(Collectors.joining(", ", "[", "]"));

            throw new GradleException(String.format(
                    "Task: %s requires one of the extensions: %s. " +
                            "To add the extensions to the project, you can run the following command:\n" +
                            "\tgradle addExtension --extensions=<extension name>",
                    getName(), availableExtensions));
        }

        File outputFile = getOutputFile().get().getAsFile();
        Files.write(outputFile.toPath(), builder().name().getBytes());
    }
}
