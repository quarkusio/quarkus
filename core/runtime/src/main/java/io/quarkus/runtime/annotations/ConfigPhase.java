package io.quarkus.runtime.annotations;

public enum ConfigPhase {
    /**
     * Values are read and available for usage at build time.
     */
    BUILD_TIME(true, false, false, "Build time"),
    /**
     * Values are read and available for usage at build time, and available on a read-only basis at run time.
     */
    BUILD_AND_RUN_TIME_FIXED(true, true, false, "Build time and run time fixed"),

    /**
     * Values are read and available for usage at run time and are re-read on each program execution. These values
     * are used to configure ConfigSourceProvider implementations
     */
    BOOTSTRAP(false, true, true, "Bootstrap"),

    /**
     * Values are read and available for usage at run time and are re-read on each program execution.
     */
    RUN_TIME(false, true, true, "Run time"),
    ;

    private final boolean availableAtBuild;
    private final boolean availableAtRun;
    private final boolean readAtMain;
    private final String name;

    ConfigPhase(final boolean availableAtBuild, final boolean availableAtRun, final boolean readAtMain, final String name) {
        this.availableAtBuild = availableAtBuild;
        this.availableAtRun = availableAtRun;
        this.readAtMain = readAtMain;
        this.name = name;
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

    @Override
    public String toString() {
        return name;
    }
}
