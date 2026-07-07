package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

import io.quarkus.gradle.application.internal.plugin.DslLifecycleCoordinator;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;

/**
 * A typed JVM startup archive associated with one named AOT-JAR output.
 * File and directory locations are deliberately separate so Gradle can track
 * their shapes without inspecting an untyped path.
 * <p>
 * Select a concrete type before using the archive. File-shaped types require {@link #getFile()}; directory-shaped
 * types require {@link #getDirectory()}. Supply the matching location directly, select {@link #fromPackageBuild()}, or
 * let one integration-test training suite produce it. These three source modes are mutually exclusive.
 */
public abstract class QuarkusApplicationJvmStartupArchive {

    private final DslLifecycleCoordinator lifecycle;
    private final String buildName;

    /**
     * Creates startup-archive state for one named AOT-JAR output.
     *
     * @param lifecycle the plugin's internal DSL lifecycle coordinator
     * @param buildName the owning build name
     */
    @Inject
    public QuarkusApplicationJvmStartupArchive(Object lifecycle, String buildName) {
        if (!(lifecycle instanceof DslLifecycleCoordinator coordinator)) {
            throw new IllegalArgumentException("Quarkus JVM startup archive requires its internal lifecycle coordinator");
        }
        this.lifecycle = coordinator;
        this.buildName = buildName;
    }

    /**
     * Returns the concrete JVM startup archive technology and file-system shape.
     *
     * @return the archive type, unset until explicitly selected or fixed by a typed registration overload
     */
    public abstract Property<QuarkusApplicationJvmStartupArchiveType> getType();

    /**
     * Returns the supplied or producer-mapped archive file for file-shaped archive types.
     *
     * @return the archive file, unset by default
     */
    public abstract RegularFileProperty getFile();

    /**
     * Returns the supplied or producer-mapped archive directory for directory-shaped archive types.
     *
     * @return the archive directory, unset by default
     */
    public abstract DirectoryProperty getDirectory();

    /**
     * Uses the selected named package operation as this archive's producer.
     * This source is mutually exclusive with a supplied file/directory and
     * integration-test training. A concrete type must already be selected. Calling this method configures the package
     * task to create the archive and fixes the matching file or directory location.
     */
    public void fromPackageBuild() {
        lifecycle.packageBuildConfigured(this, buildName);
    }
}
