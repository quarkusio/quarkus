package io.quarkus.gradle.application.tasks;

import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.execution.ImageOperation;

/**
 * Pushes the startup-optimized container image for an AOT-JAR named build.
 * <p>
 * The task consumes the archive-free base-image receipt and validated startup archive, then writes a verified optimized
 * image receipt. It always executes and is not build-cacheable because it mutates an external registry.
 * <p>
 * The supported compatibility contract covers plugin-registered instances and the documented task names, properties,
 * and options. No compatibility commitment is made for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Startup-optimized image push mutates external container image state")
public abstract class QuarkusApplicationStartupOptimizedImagePushTask
        extends QuarkusApplicationStartupOptimizedImageTask {

    /**
     * Pushes and validates the startup-optimized image.
     */
    @TaskAction
    public void pushStartupOptimizedImage() {
        executeStartupOptimizedImageOperation(ImageOperation.PUSH);
    }
}
