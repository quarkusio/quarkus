package io.quarkus.gradle.application.model;

/**
 * Runtime used by an integration-test startup-archive producer.
 */
public enum QuarkusApplicationStartupArchiveTrainingExecutionTarget {
    /**
     * Train using the host JVM.
     */
    HOST_JVM,
    /**
     * Train inside the configured base container image.
     */
    BASE_IMAGE
}
