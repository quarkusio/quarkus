package io.quarkus.runtime;

public enum LaunchMode {
    /**
     * A normal production build. At the moment this can be both native image or
     * JVM mode, but eventually these will likely be split
     */
    NORMAL(LaunchMode.PROD_PROFILE, "quarkus.profile", false, false),
    /**
     * A normal production build. At the moment this can be both native image or
     * JVM mode, but eventually these will likely be split
     */
    RUN(LaunchMode.PROD_PROFILE, "quarkus.profile", true, false),
    /**
     * quarkus:dev or an IDE launch (when we support IDE launch)
     */
    DEVELOPMENT(LaunchMode.DEV_PROFILE, "quarkus.profile", true, true),
    /**
     * a test run
     */
    TEST(LaunchMode.TEST_PROFILE, "quarkus.test.profile", true, true);

    public static final String DEV_PROFILE = "dev";
    public static final String PROD_PROFILE = "prod";
    public static final String TEST_PROFILE = "test";

    private final String defaultProfile;
    private final String profileKey;
    private final boolean devServicesSupported;
    private final boolean liveReloadSupported;

    LaunchMode(final String defaultProfile, final String profileKey, final boolean devServicesSupported,
            final boolean liveReloadSupported) {
        this.defaultProfile = defaultProfile;
        this.profileKey = profileKey;
        this.devServicesSupported = devServicesSupported;
        this.liveReloadSupported = liveReloadSupported;
    }

    public String getDefaultProfile() {
        return defaultProfile;
    }

    public String getProfileKey() {
        return profileKey;
    }

    public boolean isDev() {
        return this == DEVELOPMENT;
    }

    /**
     * Returns true if the current launch is the server side of remote dev.
     */
    public boolean isRemoteDev() {
        return (this == DEVELOPMENT) && "true".equals(System.getenv("QUARKUS_LAUNCH_DEVMODE"));
    }

    public boolean isDevOrTest() {
        return this == DEVELOPMENT || this == TEST;
    }

    /**
     * Returns true if the current launch is a production mode, such as NORMAL or RUN.
     */
    public boolean isProduction() {
        return LaunchMode.PROD_PROFILE.equals(getDefaultProfile());
    }

    public boolean isDevServicesSupported() {
        return devServicesSupported;
    }

    public boolean isDevResourcesSupported() {
        // for now, we support Dev Resources when Dev Services are supported but we have the option to split it later
        return devServicesSupported;
    }

    public boolean isLiveReloadSupported() {
        return liveReloadSupported;
    }

    private static volatile LaunchMode launchMode = LaunchMode.NORMAL;

    public static void set(LaunchMode mode) {
        launchMode = mode;
    }

    /**
     *
     * @return The current launch mode
     */
    public static LaunchMode current() {
        return launchMode;
    }
}
