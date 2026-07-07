package io.quarkus.gradle.application.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.image.ImageReferenceClaimService;
import io.quarkus.gradle.application.internal.image.ImageReferenceOwner;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolution;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolutionCodec;

/**
 * Generated preflight task that derives and claims a startup-optimized image
 * reference from the normal base-image resolution receipt.
 * <p>
 * This type is public so Gradle can decorate and register it. It is internal
 * plugin wiring rather than a supported typed user entry point; the plugin
 * owns its instances, inputs, and invocation order.
 */
@DisableCachingByDefault(because = "Container image reference claims are scoped to one Gradle invocation")
public abstract class QuarkusApplicationStartupOptimizedImageReferenceResolutionTask extends DefaultTask {

    private final ImageReferenceResolutionCodec codec = new ImageReferenceResolutionCodec();

    /**
     * Creates an always-executed invocation-scoped preflight task.
     */
    public QuarkusApplicationStartupOptimizedImageReferenceResolutionTask() {
        getOutputs().upToDateWhen(task -> false);
    }

    /**
     * Returns the named build that owns the optimized image.
     *
     * @return the build name
     */
    @Input
    public abstract Property<String> getBuildName();

    /**
     * Returns the Gradle project path that owns the claim.
     *
     * @return the owner project path
     */
    @Input
    public abstract Property<String> getOwnerProjectPath();

    /**
     * Returns the non-blank suffix appended to the base image reference.
     *
     * @return the optimized-image suffix
     */
    @Input
    public abstract Property<String> getImageSuffix();

    /**
     * Returns the normal image-preflight receipt from which the base reference
     * is read.
     *
     * @return the base resolution receipt
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getBaseResolutionReceiptFile();

    /**
     * Returns the receipt written with the derived optimized reference.
     *
     * @return the optimized resolution receipt
     */
    @OutputFile
    public abstract RegularFileProperty getResolutionReceiptFile();

    /**
     * Returns the invocation-scoped claim service.
     *
     * @return the claim service
     */
    @Internal
    public abstract Property<ImageReferenceClaimService> getClaimService();

    /**
     * Derives, records, and claims a non-colliding optimized image reference.
     */
    @TaskAction
    public void resolveReferences() {
        String suffix = getImageSuffix().getOrNull();
        if (suffix == null || suffix.isBlank()) {
            throw new IllegalArgumentException("Startup-optimized image for Quarkus AOT-JAR output '"
                    + getBuildName().get() + "' requires a non-blank image suffix");
        }
        String baseReference = codec.read(getBaseResolutionReceiptFile().get().getAsFile().toPath()).primaryReference();
        String optimizedReference = baseReference + suffix;
        if (optimizedReference.equals(baseReference)) {
            throw new IllegalArgumentException("Startup-optimized image for Quarkus AOT-JAR output '"
                    + getBuildName().get() + "' must not overwrite its archive-free base image");
        }
        ImageReferenceResolution resolution = new ImageReferenceResolution(optimizedReference, java.util.List.of());
        codec.write(getResolutionReceiptFile().get().getAsFile().toPath(), resolution);
        getClaimService().get().claim(
                new ImageReferenceOwner(getOwnerProjectPath().get(), getBuildName().get(),
                        ImageReferenceOwner.Flavor.STARTUP_OPTIMIZED),
                resolution.allReferences());
    }
}
