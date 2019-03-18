package io.quarkus.runtime.annotations;

public enum ConfigPhase {
    /**
     * Values are read and available for usage at build time.
     */
    BUILD_TIME(true, false, false),
    /**
     * Values are read and available for usage at build time, and available on a read-only basis at run time.
     */
    BUILD_AND_RUN_TIME_FIXED(true, true, false),
    /**
     * Values are read and available for usage at run time and are re-read on each program execution.
     */
    RUN_TIME(false, true, true),
    ;

    private final boolean availableAtBuild;
    private final boolean availableAtRun;
    private final boolean readAtMain;

    ConfigPhase(final boolean availableAtBuild, final boolean availableAtRun, final boolean readAtMain) {
        this.availableAtBuild = availableAtBuild;
        this.availableAtRun = availableAtRun;
        this.readAtMain = readAtMain;
    }

    public boolean isAvailableAtBuild() {
        return availableAtBuild;
    }

    public boolean isAvailableAtRun() {
        return availableAtRun;
    }

    public boolean isReadAtStaticInit() {
        return isAvailableAtBuild() && isAvailableAtRun();
    }

    public boolean isReadAtMain() {
        return readAtMain;
    }
}
