package io.quarkus.gradle.application.tasks;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.execution.ImageOperation;

/**
 * Pushes the normal container image for a named application build.
 * <p>
 * The task consumes the corresponding image-reference preflight receipt, verifies that Quarkus reports the preflight
 * reference, and writes a result receipt. It always executes and is not build-cacheable because it mutates an external
 * registry.
 * <p>
 * The supported compatibility contract covers plugin-registered instances and the documented task names, properties,
 * and options. No compatibility commitment is made for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Container image push mutates external container image state")
public abstract class QuarkusApplicationImagePushTask extends QuarkusApplicationImageTask {

    /**
     * Creates a task whose operation kind defaults to {@link ImageOperation#PUSH}.
     */
    public QuarkusApplicationImagePushTask() {
        getOperationKind().convention(ImageOperation.PUSH);
    }

    /**
     * Returns the operation kind supplied to the shared image execution machinery.
     *
     * @return the image operation kind
     */
    @Input
    public abstract Property<ImageOperation> getOperationKind();

    /**
     * Returns the image result receipt written by the task.
     *
     * @return the result receipt
     */
    @OutputFile
    public abstract RegularFileProperty getReceiptFile();

    /**
     * Returns the preflight receipt that fixes the expected primary image reference.
     *
     * @return the preflight receipt
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getImageReferencePreflightReceiptFile();

    /**
     * Pushes the image and verifies its reported reference against the preflight result.
     */
    @TaskAction
    public void pushImage() {
        executeImageOperation(getOperationKind().get(), getReceiptFile().get().getAsFile().toPath(),
                getImageReferencePreflightReceiptFile().get().getAsFile().toPath());
    }
}
