package io.quarkus.gradle.application.model;

/**
 * Concrete startup-archive technologies supported by the standalone
 * application plugin. {@link #AOT} means the OpenJDK/HotSpot AOT cache; it is
 * distinct from the historical Quarkus {@code aot-jar} package umbrella.
 */
public enum QuarkusApplicationJvmStartupArchiveType {
    /**
     * OpenJDK/HotSpot AOT cache file.
     */
    AOT("aot", "AOT", "app.aot", false, "-aot"),
    /**
     * OpenJ9 shared class-cache directory.
     */
    SCC("scc", "SCC", "app-scc", true, "-scc"),
    /**
     * HotSpot application class-data-sharing archive file.
     */
    APP_CDS("app-cds", "AppCDS", "app-cds.jsa", false, "-app-cds");

    private final String quarkusType;
    private final String coreType;
    private final String defaultName;
    private final boolean directory;
    private final String defaultImageSuffix;

    QuarkusApplicationJvmStartupArchiveType(String quarkusType, String coreType, String defaultName, boolean directory,
            String defaultImageSuffix) {
        this.quarkusType = quarkusType;
        this.coreType = coreType;
        this.defaultName = defaultName;
        this.directory = directory;
        this.defaultImageSuffix = defaultImageSuffix;
    }

    /**
     * Returns the value used by Quarkus startup-archive configuration.
     *
     * @return the Quarkus archive-type value
     */
    public String getQuarkusType() {
        return quarkusType;
    }

    /**
     * Returns the conventional file or directory name.
     *
     * @return the default archive name
     */
    public String getDefaultName() {
        return defaultName;
    }

    /**
     * Returns the build-tool-neutral core archive type.
     *
     * @return the core archive-type name
     */
    public String getCoreType() {
        return coreType;
    }

    /**
     * Returns whether this archive is represented by a directory.
     *
     * @return {@code true} for directory-shaped archives
     */
    public boolean isDirectory() {
        return directory;
    }

    /**
     * Returns the default suffix for a startup-optimized image.
     *
     * @return the suffix including its leading hyphen
     */
    public String getDefaultImageSuffix() {
        return defaultImageSuffix;
    }
}
