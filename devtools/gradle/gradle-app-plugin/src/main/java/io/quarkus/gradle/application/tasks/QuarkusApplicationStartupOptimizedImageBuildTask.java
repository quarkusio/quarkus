package io.quarkus.gradle.application.tasks;

import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.execution.ImageOperation;

/**
 * Builds the startup-optimized container image for an AOT-JAR named build.
 * <p>
 * The task consumes the archive-free base-image receipt and validated startup archive, then writes a verified optimized
 * image receipt. It always executes and is not build-cacheable because it mutates external container-image state.
 * <p>
 * The supported compatibility contract covers plugin-registered instances and the documented task names, properties,
 * and options. No compatibility commitment is made for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Startup-optimized image build mutates external container image state")
public abstract class QuarkusApplicationStartupOptimizedImageBuildTask
        extends QuarkusApplicationStartupOptimizedImageTask {

    /**
     * Builds and validates the startup-optimized image.
     */
    @TaskAction
    public void buildStartupOptimizedImage() {
        executeStartupOptimizedImageOperation(ImageOperation.BUILD);
    }
}
