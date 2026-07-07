package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

/**
 * A named Quarkus legacy-JAR package output with runner naming, manifest, image, deployment, run, and integration-test
 * support.
 */
public abstract class QuarkusLegacyJarOutput extends QuarkusApplicationRunnerOutput
        implements QuarkusApplicationJarOutput {

    /**
     * Creates the Gradle-managed legacy-JAR output.
     *
     * @param name the build name
     * @param layout the project layout
     * @param objects Gradle's object factory
     */
    @Inject
    public QuarkusLegacyJarOutput(String name, ProjectLayout layout, ObjectFactory objects) {
        super(name, QuarkusApplicationBuildType.LEGACY_JAR, objects, layout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QuarkusApplicationManifest getManifest() {
        return manifest();
    }
}
