package io.quarkus.deployment.pkg.builditem;

/**
 * Represents the type of JVM startup archive to be produced
 */
public enum JvmStartupOptimizerArchiveType {
    AppCDS("-XX:SharedArchiveFile"),
    AOT("-XX:AOTCache");

    private final String jvmFlag;

    JvmStartupOptimizerArchiveType(String jvmFlag) {
        this.jvmFlag = jvmFlag;
    }

    public String getJvmFlag() {
        return jvmFlag;
    }
}
