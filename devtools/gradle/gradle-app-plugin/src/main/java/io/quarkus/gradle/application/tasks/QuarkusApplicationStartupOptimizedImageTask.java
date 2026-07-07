package io.quarkus.gradle.application.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.execution.ImageOperation;
import io.quarkus.gradle.application.internal.execution.StartupOptimizedImageRequest;
import io.quarkus.gradle.application.internal.image.BuiltContainerImage;
import io.quarkus.gradle.application.internal.image.BuiltContainerImageResultCodec;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolutionCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;

/**
 * Implementation base for startup-optimized container-image build and push tasks.
 * <p>
 * Public visibility is required for Gradle decoration. Plugin registration supplies a base-image receipt, startup
 * archive, image suffix, and reference-preflight receipt. This base validates those inputs, prevents overwriting the
 * archive-free base image, verifies the reported optimized reference, and records the result. It is not a supported
 * typed user entry point and makes no compatibility commitment for direct construction, additional registration, or
 * subclassing.
 */
@DisableCachingByDefault(because = "Startup-optimized image tasks mutate external container image state")
public abstract class QuarkusApplicationStartupOptimizedImageTask extends QuarkusApplicationBuildTask {

    private final BuiltContainerImageResultCodec resultCodec = new BuiltContainerImageResultCodec();
    private final ImageReferenceResolutionCodec resolutionCodec = new ImageReferenceResolutionCodec();

    /**
     * Creates a startup-optimized image task that always executes when selected.
     */
    public QuarkusApplicationStartupOptimizedImageTask() {
        getOutputs().upToDateWhen(task -> false);
    }

    /**
     * Returns the concrete startup archive type.
     *
     * @return the optional archive type; plugin registration supplies a value for executable tasks
     */
    @Input
    @Optional
    public abstract Property<QuarkusApplicationJvmStartupArchiveType> getArchiveType();

    /**
     * Returns the startup archive file for file-based archive types.
     *
     * @return the optional archive file
     */
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getArchiveFile();

    /**
     * Returns the startup archive directory for directory-based archive types.
     *
     * @return the optional archive directory
     */
    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getArchiveDirectory();

    /**
     * Returns the non-blank suffix appended to the base image reference.
     *
     * @return the optional image suffix; plugin registration supplies a value for executable tasks
     */
    @Input
    @Optional
    public abstract Property<String> getImageSuffix();

    /**
     * Returns the container-image builder selected for this operation.
     *
     * @return the optional image builder
     */
    @Input
    @Optional
    public abstract Property<QuarkusApplicationImageBuilder> getImageBuilder();

    /**
     * Returns the receipt for the archive-free base image.
     *
     * @return the base-image receipt
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getBaseImageReceiptFile();

    /**
     * Returns the preflight receipt containing the expected optimized image reference.
     *
     * @return the image-reference preflight receipt
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getImageReferencePreflightReceiptFile();

    /**
     * Returns the result receipt written after the optimized image operation.
     *
     * @return the result receipt
     */
    @OutputFile
    public abstract RegularFileProperty getReceiptFile();

    /**
     * Validates inputs, executes the optimized image operation, and verifies its reported reference.
     *
     * @param operation build or push
     */
    protected void executeStartupOptimizedImageOperation(ImageOperation operation) {
        Path baseReceipt = getBaseImageReceiptFile().get().getAsFile().toPath();
        if (!Files.isRegularFile(baseReceipt)) {
            throw new GradleException("Startup-optimized image task requires base image receipt " + baseReceipt
                    + ", but the file does not exist");
        }
        BuiltContainerImage baseImage = resultCodec.read(baseReceipt);
        String baseReference = baseImage.reference()
                .orElseThrow(() -> new GradleException("Base image receipt " + baseReceipt
                        + " does not contain an image reference"));
        baseImage.workingDirectory()
                .orElseThrow(() -> new GradleException("Base image receipt " + baseReceipt
                        + " does not contain an image working directory"));

        QuarkusApplicationJvmStartupArchiveType archiveType = getArchiveType().getOrNull();
        if (archiveType == null) {
            throw new GradleException("Startup-optimized image for Quarkus AOT-JAR output '" + getBuildName().get()
                    + "' requires a concrete startup archive type");
        }
        if (!getImageSuffix().isPresent() || getImageSuffix().get().isBlank()) {
            throw new GradleException("Startup-optimized image for Quarkus AOT-JAR output '" + getBuildName().get()
                    + "' requires a non-blank image suffix");
        }
        Path archive = validateArchive(archiveType);
        String optimizedReference = optimizedReference(baseReference);
        String preflightReference = resolutionCodec
                .read(getImageReferencePreflightReceiptFile().get().getAsFile().toPath())
                .primaryReference();
        if (!optimizedReference.equals(preflightReference)) {
            throw new GradleException("Startup-optimized image operation for named build '" + getBuildName().get()
                    + "' would request image '" + optimizedReference + "', but its preflight resolved '"
                    + preflightReference + "'");
        }

        Map<String, String> operationForcedProperties = startupOptimizedImageOperationProperties(operation);
        BuiltContainerImage image = switch (operation) {
            case BUILD -> buildOperations().buildStartupOptimizedImage(request(operation, baseImage, baseReceipt,
                    archiveType, archive, operationForcedProperties, optimizedReference));
            case PUSH -> buildOperations().pushStartupOptimizedImage(request(operation, baseImage, baseReceipt,
                    archiveType, archive, operationForcedProperties, optimizedReference));
        };
        String actualReference = image.reference()
                .orElseThrow(() -> new GradleException("Startup-optimized image operation for named build '"
                        + getBuildName().get() + "' did not report an image reference"));
        if (!preflightReference.equals(actualReference)) {
            throw new GradleException("Startup-optimized image operation for named build '" + getBuildName().get()
                    + "' reported image '" + actualReference + "', but its preflight resolved '"
                    + preflightReference + "'");
        }
        resultCodec.write(getReceiptFile().get().getAsFile().toPath(), image);
    }

    private StartupOptimizedImageRequest request(ImageOperation operation,
            BuiltContainerImage baseImage, Path baseReceipt, QuarkusApplicationJvmStartupArchiveType archiveType,
            Path archive, Map<String, String> operationForcedProperties, String optimizedReference) {
        return new StartupOptimizedImageRequest(
                buildRequest(operationForcedProperties),
                operation,
                baseImage,
                baseReceipt,
                archiveType,
                archive,
                optimizedReference,
                java.util.Optional.ofNullable(getImageBuilder().getOrNull()),
                getReceiptFile().get().getAsFile().toPath());
    }

    private Map<String, String> startupOptimizedImageOperationProperties(ImageOperation operation) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("quarkus.container-image.build", "true");
        properties.put("quarkus.container-image.push", Boolean.toString(operation == ImageOperation.PUSH));
        if (getImageBuilder().isPresent()) {
            properties.put("quarkus.container-image.builder", getImageBuilder().get().quarkusBuilderName());
        }
        properties.put("quarkus.container-image.aot-image-suffix", getImageSuffix().get());
        return properties;
    }

    private String optimizedReference(String baseReference) {
        String reference = baseReference + getImageSuffix().get();
        if (reference.equals(baseReference)) {
            throw new GradleException("Startup-optimized image for Quarkus AOT-JAR output '" + getBuildName().get()
                    + "' must not overwrite its archive-free base image");
        }
        return reference;
    }

    private Path validateArchive(QuarkusApplicationJvmStartupArchiveType type) {
        if (type.isDirectory()) {
            if (getArchiveFile().isPresent() || !getArchiveDirectory().isPresent()) {
                throw new GradleException("Startup archive type " + type + " requires one directory and no file");
            }
            Path directory = getArchiveDirectory().get().getAsFile().toPath();
            if (!Files.isDirectory(directory)) {
                throw new GradleException("Startup archive directory " + directory + " does not exist");
            }
            try (var children = Files.list(directory)) {
                if (children.findAny().isEmpty()) {
                    throw new GradleException("Startup archive directory " + directory + " is empty");
                }
            } catch (java.io.IOException e) {
                throw new GradleException("Failed to inspect startup archive directory " + directory, e);
            }
            return directory;
        }
        if (getArchiveDirectory().isPresent() || !getArchiveFile().isPresent()) {
            throw new GradleException("Startup archive type " + type + " requires one file and no directory");
        }
        Path file = getArchiveFile().get().getAsFile().toPath();
        if (!Files.isRegularFile(file)) {
            throw new GradleException("Startup archive file " + file + " does not exist");
        }
        try {
            if (Files.size(file) == 0) {
                throw new GradleException("Startup archive file " + file + " is empty");
            }
        } catch (java.io.IOException e) {
            throw new GradleException("Failed to inspect startup archive file " + file, e);
        }
        return file;
    }
}
