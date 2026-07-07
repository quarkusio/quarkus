package io.quarkus.gradle.application.dsl;

import org.gradle.api.provider.Property;

import io.quarkus.gradle.application.model.QuarkusApplicationStartupArchiveTrainingExecutionTarget;

/**
 * Opts a Gradle JVM integration-test suite into producing the selected
 * AOT-JAR output's startup archive.
 * <p>
 * The suite must also be configured as Quarkus integration tests for an AOT-JAR build. Exactly one execution target is
 * required. Training cannot use AppCDS, cannot share one AOT-JAR output with another training suite, and is mutually
 * exclusive with {@link QuarkusApplicationJvmStartupArchive#fromPackageBuild()} and user-supplied archive locations.
 */
public abstract class QuarkusApplicationStartupArchiveTraining {

    /**
     * Returns where the integration-test workload executes while producing the archive.
     *
     * @return the required execution target, with no convention
     */
    public abstract Property<QuarkusApplicationStartupArchiveTrainingExecutionTarget> getExecutionTarget();
}
