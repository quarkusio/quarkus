package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;

import io.quarkus.gradle.application.internal.plugin.DslLifecycleCoordinator;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

/**
 * A named Quarkus {@code aot-jar} package output.
 * <p>
 * The historical package name describes the Quarkus layout, not the concrete
 * startup technology. Its startup archive may be an OpenJDK AOT cache, an
 * OpenJ9 shared classes cache, or an AppCDS archive. A concrete archive type and exactly one archive producer or
 * supplied location are required only when an archive consumer, such as a startup-optimized image, is selected.
 */
public abstract class QuarkusAotJarOutput extends QuarkusApplicationBuild implements QuarkusApplicationJarOutput {

    private final QuarkusApplicationJvmStartupArchive startupArchive;
    private final QuarkusApplicationStartupOptimizedImage startupOptimizedImage;
    private final DslLifecycleCoordinator lifecycle;

    /**
     * Creates the Gradle-managed AOT-JAR output and its provider-backed startup-archive blocks.
     *
     * @param name the build name
     * @param layout the project layout
     * @param objects Gradle's object factory
     * @param lifecycle the plugin's internal DSL lifecycle coordinator
     */
    @Inject
    public QuarkusAotJarOutput(String name, ProjectLayout layout, ObjectFactory objects,
            Object lifecycle) {
        super(name, QuarkusApplicationBuildType.AOT_JAR, objects, layout);
        if (!(lifecycle instanceof DslLifecycleCoordinator coordinator)) {
            throw new IllegalArgumentException("Quarkus AOT-JAR output requires its internal lifecycle coordinator");
        }
        this.lifecycle = coordinator;
        startupArchive = objects.newInstance(QuarkusApplicationJvmStartupArchive.class, coordinator, name);
        startupOptimizedImage = objects.newInstance(QuarkusApplicationStartupOptimizedImage.class);
        startupOptimizedImage.getImageSuffix()
                .convention(startupArchive.getType().map(type -> type.getDefaultImageSuffix()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QuarkusApplicationManifest getManifest() {
        return manifest();
    }

    /**
     * Returns the typed startup archive consumed by optional downstream
     * operations. Creating an AOT-JAR output does not select or produce an
     * archive by itself.
     *
     * @return the startup-archive configuration
     */
    public QuarkusApplicationJvmStartupArchive getStartupArchive() {
        return startupArchive;
    }

    /**
     * Configures the typed startup archive without selecting a producer by itself.
     *
     * @param action the startup-archive configuration action
     */
    public void startupArchive(Action<? super QuarkusApplicationJvmStartupArchive> action) {
        action.execute(startupArchive);
    }

    /**
     * Returns the startup-optimized image configuration.
     * <p>
     * Reading the object does not register optimized image tasks; call
     * {@link #startupOptimizedImage(Action)} to select that consumer.
     *
     * @return the startup-optimized image configuration
     */
    public QuarkusApplicationStartupOptimizedImage getStartupOptimizedImage() {
        return startupOptimizedImage;
    }

    /**
     * Configures the optional startup-optimized image consumer. Calling this
     * method registers that output's startup-optimized build and push tasks.
     *
     * @param action the startup-optimized image configuration action
     */
    public void startupOptimizedImage(Action<? super QuarkusApplicationStartupOptimizedImage> action) {
        action.execute(startupOptimizedImage);
        lifecycle.startupOptimizedImageConfigured(this);
    }
}
