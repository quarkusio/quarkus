package io.quarkus.gradle.application.tasks;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

/**
 * Implementation base carrying the immutable identity and output location of a plugin-created named application task.
 * <p>
 * Public visibility is required for Gradle decoration. This abstract type is not a supported typed user entry point and
 * makes no compatibility commitment for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Base named application task has no standalone cacheable behavior")
public abstract class QuarkusApplicationTask extends QuarkusApplicationBaseTask {

    /**
     * Returns the named build that owns this task.
     *
     * @return the required build name
     */
    @Input
    public abstract Property<String> getBuildName();

    /**
     * Returns the fixed output type of the owning named build.
     *
     * @return the required build type
     */
    @Input
    public abstract Property<QuarkusApplicationBuildType> getBuildType();

    /**
     * Returns the optional package output name.
     *
     * @return the output name, or no value when the concrete task does not require one
     */
    @Input
    @Optional
    public abstract Property<String> getOutputName();

    /**
     * Returns the operation-owned output directory.
     * <p>
     * Concrete task types redeclare it as an output where appropriate.
     *
     * @return the output directory
     */
    @Internal
    public abstract DirectoryProperty getOutputDirectory();

}
