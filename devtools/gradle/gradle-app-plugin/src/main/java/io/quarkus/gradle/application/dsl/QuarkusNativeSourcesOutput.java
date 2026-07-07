package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

/**
 * A named output that produces the sources and metadata required for a later native-image build.
 */
public abstract class QuarkusNativeSourcesOutput extends QuarkusApplicationRunnerOutput {

    /**
     * Creates the Gradle-managed native-sources output.
     *
     * @param name the build name
     * @param layout the project layout
     * @param objects Gradle's object factory
     */
    @Inject
    public QuarkusNativeSourcesOutput(String name, ProjectLayout layout, ObjectFactory objects) {
        super(name, QuarkusApplicationBuildType.NATIVE_SOURCES, objects, layout);
    }
}
