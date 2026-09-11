package io.quarkus.gradle.application.tasks;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.execution.ImageOperation;
import io.quarkus.gradle.application.internal.image.ImageReferenceClaimService;
import io.quarkus.gradle.application.internal.image.ImageReferenceOwner;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolution;

/**
 * Generated preflight task that resolves and claims the references affected by
 * one named image operation before that operation has side effects.
 * <p>
 * This type is public so Gradle can decorate and register it. It is internal
 * plugin wiring rather than a supported typed user entry point; the plugin
 * owns its instances, inputs, and invocation order.
 */
@DisableCachingByDefault(because = "Container image reference claims are scoped to one Gradle invocation")
public abstract class QuarkusApplicationImageReferenceResolutionTask extends QuarkusApplicationImageTask {

    /**
     * Creates an always-executed invocation-scoped preflight task.
     */
    public QuarkusApplicationImageReferenceResolutionTask() {
        getOutputs().upToDateWhen(task -> false);
    }

    /**
     * Returns the internal image operation whose references are claimed.
     *
     * @return the operation kind
     */
    @Input
    public abstract Property<ImageOperation> getOperationKind();

    /**
     * Returns the Gradle project path that owns the claim.
     *
     * @return the owner project path
     */
    @Input
    public abstract Property<String> getOwnerProjectPath();

    /**
     * Returns the receipt written with the resolved primary and additional
     * image references.
     *
     * @return the resolution receipt
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
     * Resolves the effective references, writes their receipt, and claims them
     * before the corresponding image operation executes.
     */
    @TaskAction
    public void resolveReferences() {
        ImageReferenceResolution resolution = buildOperations().resolveImageReferences(
                imageRequest(getOperationKind().get(), getResolutionReceiptFile().get().getAsFile().toPath()));
        getClaimService().get().claim(
                new ImageReferenceOwner(getOwnerProjectPath().get(), getBuildName().get(),
                        ImageReferenceOwner.Flavor.NORMAL),
                resolution.allReferences());
    }
}
