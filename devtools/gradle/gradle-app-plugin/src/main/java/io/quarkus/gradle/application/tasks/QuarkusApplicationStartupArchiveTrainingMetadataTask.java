package io.quarkus.gradle.application.tasks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.gradle.application.internal.image.BuiltContainerImageResultCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;
import io.quarkus.gradle.application.model.QuarkusApplicationStartupArchiveTrainingExecutionTarget;

/**
 * Generated implementation task that augments integration-test launcher metadata with startup-archive training
 * settings.
 * <p>
 * Public visibility is required for Gradle decoration. The plugin owns this task's instances and wires it only for a
 * selected AOT-JAR integration-test training producer. It is not a supported typed user entry point and makes no
 * compatibility commitment for direct construction, additional registration, or subclassing. The metadata is cheap to
 * regenerate and is not build-cached.
 */
@DisableCachingByDefault(because = "Integration-test launch metadata is cheap to regenerate")
public abstract class QuarkusApplicationStartupArchiveTrainingMetadataTask extends DefaultTask {

    private static final String QUARKUS_ARTIFACT_PROPERTIES = "quarkus-artifact.properties";

    /**
     * Returns the package launch metadata copied into the training metadata output.
     *
     * @return the base metadata directory
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getBaseMetadataDirectory();

    /**
     * Returns the concrete archive type being trained.
     *
     * @return the archive type
     */
    @Input
    public abstract Property<QuarkusApplicationJvmStartupArchiveType> getArchiveType();

    /**
     * Returns whether training runs on the host JVM or in the archive-free base image.
     *
     * @return the execution target
     */
    @Input
    public abstract Property<QuarkusApplicationStartupArchiveTrainingExecutionTarget> getExecutionTarget();

    /**
     * Returns the target-visible destination where the training process writes the archive.
     *
     * @return the archive destination
     */
    @Input
    public abstract Property<String> getArchiveDestination();

    /**
     * Returns the validated test-suite segment used to identify the training operation.
     *
     * @return the suite path segment
     */
    @Input
    public abstract Property<String> getSuitePathSegment();

    /**
     * Returns the base-image receipt required only for base-image training.
     *
     * @return the optional base-image receipt
     */
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getBaseImageReceiptFile();

    /**
     * Returns the complete training launcher metadata directory owned by this task.
     *
     * @return the metadata directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getMetadataDirectory();

    /**
     * Copies base launch metadata and adds validated archive-training target information.
     */
    @TaskAction
    public void generateMetadata() {
        Path sourceFile = getBaseMetadataDirectory().file(QUARKUS_ARTIFACT_PROPERTIES).get().getAsFile().toPath();
        Properties source = new Properties();
        try (var reader = Files.newBufferedReader(sourceFile)) {
            source.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read integration-test metadata " + sourceFile, e);
        }

        Map<String, String> target = new LinkedHashMap<>();
        source.stringPropertyNames().stream().sorted()
                .forEach(key -> target.put(key, source.getProperty(key)));
        QuarkusApplicationJvmStartupArchiveType archiveType = getArchiveType().get();
        QuarkusApplicationStartupArchiveTrainingExecutionTarget executionTarget = getExecutionTarget().get();
        Path destination = Path.of(getArchiveDestination().get()).toAbsolutePath().normalize();
        target.put("metadata.jvm-startup-archive.type", archiveType.getCoreType());
        target.put("metadata.jvm-startup-archive.destination", destination.toString());
        target.put("metadata.jvm-startup-archive.execution-target", executionTarget.name());

        if (executionTarget == QuarkusApplicationStartupArchiveTrainingExecutionTarget.BASE_IMAGE) {
            if (!getBaseImageReceiptFile().isPresent()) {
                throw new GradleException("Base-image startup-archive training requires a base image receipt");
            }
            Path receipt = getBaseImageReceiptFile().get().getAsFile().toPath();
            var image = new BuiltContainerImageResultCodec().read(receipt);
            String reference = image.reference()
                    .orElseThrow(() -> new GradleException("Base image receipt " + receipt
                            + " does not contain an image reference"));
            String workingDirectory = image.workingDirectory()
                    .orElseThrow(() -> new GradleException("Base image receipt " + receipt
                            + " does not contain an image working directory"));
            String containerDirectory = containerTrainingDirectory(workingDirectory, getSuitePathSegment().get());
            target.put("type", "jar-container");
            target.put("metadata.container-image", reference);
            target.put("metadata.working-directory", workingDirectory);
            target.put("metadata.output-directory", destination.getParent().toString());
            target.put("metadata.jvm-startup-archive.container-directory", containerDirectory);
        }

        Path metadataDirectory = getMetadataDirectory().get().getAsFile().toPath();
        try {
            Files.createDirectories(metadataDirectory);
            PropertyUtils.store(target, metadataDirectory.resolve(QUARKUS_ARTIFACT_PROPERTIES));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write startup-archive training metadata in "
                    + metadataDirectory, e);
        }
    }

    private static String containerTrainingDirectory(String workingDirectory, String suitePathSegment) {
        String normalized = workingDirectory.endsWith("/") && workingDirectory.length() > 1
                ? workingDirectory.substring(0, workingDirectory.length() - 1)
                : workingDirectory;
        return ("/".equals(normalized) ? "" : normalized)
                + "/.quarkus-startup-archive-training/" + suitePathSegment;
    }
}
