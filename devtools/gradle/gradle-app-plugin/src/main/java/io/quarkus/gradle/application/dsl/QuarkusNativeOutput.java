package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

/**
 * A named native-executable output.
 * <p>
 * Registration creates the native build task and a matching, separately selected native-test suite/task. It does not
 * add either task to {@code assemble}, {@code build}, or {@code check} unless the build explicitly opts into
 * {@link #getParticipatesInAssemble()} for its primary native build.
 */
public abstract class QuarkusNativeOutput extends QuarkusApplicationRunnerOutput {

    /**
     * Creates the Gradle-managed native-executable output.
     *
     * @param name the build name
     * @param layout the project layout
     * @param objects Gradle's object factory
     */
    @Inject
    public QuarkusNativeOutput(String name, ProjectLayout layout, ObjectFactory objects) {
        super(name, QuarkusApplicationBuildType.NATIVE_EXECUTABLE, objects, layout);
    }
}
