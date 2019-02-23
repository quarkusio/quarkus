package io.quarkus.runtime.annotations;

public enum ConfigPhase {
    /**
     * Values are read and available for usage at build time.
     */
    BUILD_TIME(true, false, false, false),
    /**
     * Values are read and available for usage at build time, and available on a read-only basis at run time.
     */
    BUILD_AND_RUN_TIME_FIXED(true, true, false, false),
    /**
     * Values are read and available for usage at run time during static initialization. In a JVM image, they will
     * be read on every execution; in a native image, they will only be read during the building of the image.
     *
     * @deprecated We are removing static init time configuration processing.
     */
    @Deprecated
    RUN_TIME_STATIC(false, true, true, false),
    /**
     * Values are read and available for usage at run time and are re-read on each program execution.
     */
    RUN_TIME(false, true, true, true),
    ;

    private final boolean availableAtBuild;
    private final boolean availableAtRun;
    private final boolean readAtStaticInit;
    private final boolean readAtMain;

    ConfigPhase(final boolean availableAtBuild, final boolean availableAtRun, final boolean readAtStaticInit,
            final boolean readAtMain) {
        this.availableAtBuild = availableAtBuild;
        this.availableAtRun = availableAtRun;
        this.readAtStaticInit = readAtStaticInit;
        this.readAtMain = readAtMain;
    }

    public boolean isAvailableAtBuild() {
        return availableAtBuild;
    }

    public boolean isAvailableAtRun() {
        return availableAtRun;
    }

    public boolean isReadAtStaticInit() {
        return readAtStaticInit;
    }

    public boolean isReadAtMain() {
        return readAtMain;
    }
}
