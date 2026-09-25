package io.quarkus.gradle.application.tasks;

import java.nio.file.Path;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.execution.ImageOperation;
import io.quarkus.gradle.application.internal.execution.ImageRequest;
import io.quarkus.gradle.application.internal.image.BuiltContainerImage;
import io.quarkus.gradle.application.internal.image.BuiltContainerImageResultCodec;
import io.quarkus.gradle.application.internal.image.ContainerImageTarget;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolution;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolutionCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;

/**
 * Implementation base for normal container-image build and push tasks.
 * <p>
 * Public visibility is required for Gradle decoration. Plugin registration supplies the named-build configuration and
 * preflight receipt; this base validates mutually exclusive image selectors, translates them into forced Quarkus
 * properties, verifies the reported reference, and records the result. It is not a supported typed user entry point and
 * makes no compatibility commitment for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Container image tasks mutate external container image state")
public abstract class QuarkusApplicationImageTask extends QuarkusApplicationBuildTask {

    private final BuiltContainerImageResultCodec resultCodec = new BuiltContainerImageResultCodec();
    private final ImageReferenceResolutionCodec resolutionCodec = new ImageReferenceResolutionCodec();

    /**
     * Creates an image task that always executes when selected.
     */
    public QuarkusApplicationImageTask() {
        getOutputs().upToDateWhen(task -> false);
    }

    /**
     * Returns the complete image reference, when configured.
     * <p>
     * A complete reference is mutually exclusive with {@link #getImageRepository()} and {@link #getImageTag()}.
     *
     * @return the optional complete image reference
     */
    @Input
    @Optional
    public abstract Property<String> getImageReference();

    /**
     * Returns the image repository, when configured.
     *
     * @return the optional image repository
     */
    @Input
    @Optional
    public abstract Property<String> getImageRepository();

    /**
     * Returns the image tag, when configured.
     * <p>
     * When a repository is set without a tag, the application version is used and must not be {@code unspecified}.
     *
     * @return the optional image tag
     */
    @Input
    @Optional
    public abstract Property<String> getImageTag();

    /**
     * Returns the container-image builder selected for this image operation.
     *
     * @return the optional image builder
     */
    @Input
    @Optional
    public abstract Property<QuarkusApplicationImageBuilder> getImageBuilder();

    /**
     * Returns additional Quarkus build properties scoped to image operations.
     *
     * @return the image-operation properties
     */
    @Input
    public abstract MapProperty<String, String> getImageQuarkusBuildProperties();

    /**
     * Executes a normal image operation and writes a verified result receipt.
     *
     * @param operation build or push
     * @param receiptFile result receipt to write
     * @param preflightReceiptFile preflight receipt containing the expected reference
     */
    protected void executeImageOperation(ImageOperation operation, Path receiptFile, Path preflightReceiptFile) {
        ImageRequest request = imageRequest(operation, receiptFile);
        ImageReferenceResolution preflight = resolutionCodec.read(preflightReceiptFile);

        BuiltContainerImage image = switch (operation) {
            case BUILD -> buildOperations().buildImage(request);
            case PUSH -> buildOperations().pushImage(request);
        };
        String actualReference = image.reference()
                .orElseThrow(() -> new GradleException("Quarkus image operation for '" + getBuildName().get()
                        + "' did not report an image reference"));
        if (!preflight.primaryReference().equals(actualReference)) {
            throw new GradleException("Quarkus image operation for named build '" + getBuildName().get()
                    + "' reported image '" + actualReference + "', but its preflight resolved '"
                    + preflight.primaryReference() + "'");
        }
        resultCodec.write(receiptFile, image);
    }

    /**
     * Creates the image-operation request after validating image selectors.
     *
     * @param operation build or push
     * @param receiptFile result receipt to write
     * @return the operation request
     */
    protected ImageRequest imageRequest(ImageOperation operation, Path receiptFile) {
        validateImageTarget();
        Map<String, String> operationForcedProperties = imageOperationProperties(operation);
        return new ImageRequest(
                buildRequest(operationForcedProperties),
                operation,
                containerImageTarget(),
                java.util.Optional.ofNullable(getImageBuilder().getOrNull()),
                getQuarkusBuildProperties().get(),
                getImageQuarkusBuildProperties().get(),
                receiptFile,
                java.util.Optional.empty(),
                java.util.Optional.empty());
    }

    /**
     * Rejects a complete image reference combined with repository or tag configuration.
     */
    protected void validateImageTarget() {
        if (getImageReference().isPresent()
                && (getImageRepository().isPresent() || getImageTag().isPresent())) {
            throw new GradleException("Quarkus application image reference cannot be combined with repository or tag");
        }
    }

    /**
     * Creates Quarkus properties forced by the selected operation and image configuration.
     *
     * @param operation build or push
     * @return the forced operation properties
     */
    protected Map<String, String> imageOperationProperties(ImageOperation operation) {
        Map<String, String> properties = new java.util.LinkedHashMap<>();
        properties.put("quarkus.container-image.build",
                Boolean.toString(operation == ImageOperation.BUILD));
        properties.put("quarkus.container-image.push",
                Boolean.toString(operation == ImageOperation.PUSH));
        if (getImageBuilder().isPresent()) {
            properties.put("quarkus.container-image.builder", getImageBuilder().get().quarkusBuilderName());
        }
        if (getBuildType().get() == QuarkusApplicationBuildType.AOT_JAR) {
            properties.put("quarkus.package.jar.aot.enabled", "false");
        }
        if (getImageReference().isPresent()) {
            properties.put("quarkus.container-image.image", getImageReference().get());
        } else if (getImageRepository().isPresent()) {
            properties.put("quarkus.container-image.image", repositoryImageReference());
        } else if (getImageTag().isPresent()) {
            properties.put("quarkus.container-image.tag", getImageTag().get());
        }
        return properties;
    }

    /**
     * Returns the explicit target passed to image builders, when image or repository configuration supplies one.
     *
     * @return the optional image target
     */
    protected java.util.Optional<ContainerImageTarget> containerImageTarget() {
        if (getImageReference().isPresent()) {
            return java.util.Optional.of(new ContainerImageTarget(getImageReference().get()));
        }
        if (getImageRepository().isPresent()) {
            return java.util.Optional.of(new ContainerImageTarget(repositoryImageReference()));
        }
        return java.util.Optional.empty();
    }

    private String repositoryImageReference() {
        String tag = getImageTag().getOrElse(defaultImageTag());
        return getImageRepository().get() + ":" + tag;
    }

    private String defaultImageTag() {
        String version = getApplicationVersion().getOrNull();
        if (version == null || "unspecified".equals(version)) {
            throw new IllegalArgumentException(
                    "Image tag defaults to project.version, but project.version is unspecified. "
                            + "Configure image.tag or project.version.");
        }
        return version;
    }
}
