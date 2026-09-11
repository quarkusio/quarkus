package io.quarkus.deployment.pkg.builditem;

/**
 * Identifies a JVM startup optimizer archive and the contract needed to store and consume it.
 */
public enum JvmStartupOptimizerArchiveType {
    /**
     * A HotSpot application class-data sharing archive stored as a file.
     */
    AppCDS("-XX:SharedArchiveFile", "app-cds.jsa", JvmStartupOptimizerArchiveKind.FILE),

    /**
     * An OpenJDK AOT cache stored as a file.
     */
    AOT("-XX:AOTCache", "app.aot", JvmStartupOptimizerArchiveKind.FILE),

    /**
     * An OpenJ9 shared classes cache stored as a directory.
     */
    SCC("-Xshareclasses:cacheDir", "app-scc", JvmStartupOptimizerArchiveKind.DIRECTORY);

    private final String jvmFlag;
    private final String defaultName;
    private final JvmStartupOptimizerArchiveKind artifactKind;

    JvmStartupOptimizerArchiveType(String jvmFlag, String defaultName,
            JvmStartupOptimizerArchiveKind artifactKind) {
        this.jvmFlag = jvmFlag;
        this.defaultName = defaultName;
        this.artifactKind = artifactKind;
    }

    /**
     * @return the JVM option name associated with this archive type
     */
    public String getJvmFlag() {
        return jvmFlag;
    }

    /**
     * @return the default file or directory name for this archive type
     */
    public String getDefaultName() {
        return defaultName;
    }

    /**
     * @return the required filesystem shape of this archive type
     */
    public JvmStartupOptimizerArchiveKind getArtifactKind() {
        return artifactKind;
    }

    /**
     * Renders the JVM option needed to consume this archive.
     *
     * @param archivePath the path visible to the JVM that consumes the archive
     * @return the complete JVM option
     */
    public String renderRuntimeOption(String archivePath) {
        return switch (this) {
            case AppCDS, AOT -> jvmFlag + "=" + archivePath;
            case SCC -> "-Xshareclasses:name=quarkus-app,cacheDir=" + archivePath + ",readonly";
        };
    }
}
