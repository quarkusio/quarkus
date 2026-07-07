package io.quarkus.gradle.application.dsl;

import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

/**
 * Base DSL type for named outputs whose final executable may use Quarkus's runner suffix convention.
 * <p>
 * The runner suffix convention is {@code -runner}, and adding it is enabled by default. These typed values override
 * conflicting generic Quarkus build properties for the named build.
 */
public abstract class QuarkusApplicationRunnerOutput extends QuarkusApplicationBuild {

    /**
     * Creates common runner-output state.
     *
     * @param name the build name
     * @param buildType the fixed build type
     * @param objects Gradle's object factory
     * @param layout the project layout
     */
    protected QuarkusApplicationRunnerOutput(String name, QuarkusApplicationBuildType buildType, ObjectFactory objects,
            ProjectLayout layout) {
        super(name, buildType, objects, layout);
    }

    /**
     * Returns the suffix appended to runner artifacts when suffixing is enabled.
     *
     * @return the runner suffix, conventionally {@code -runner}
     */
    public abstract Property<String> getArchiveRunnerSuffix();

    /**
     * Returns whether Quarkus adds the runner suffix to the final artifact name.
     *
     * @return whether the suffix is added; the convention is {@code true}
     */
    public abstract Property<Boolean> getArchiveAddRunnerSuffix();
}
