package io.quarkus.deployment.pkg.builditem;

/**
 * Describes the filesystem shape of a JVM startup optimizer archive.
 */
public enum JvmStartupOptimizerArchiveKind {
    /**
     * A single regular file.
     */
    FILE,

    /**
     * A directory whose contents collectively form the archive.
     */
    DIRECTORY
}
