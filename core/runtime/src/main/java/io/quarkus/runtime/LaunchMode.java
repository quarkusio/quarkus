package io.quarkus.runtime;

public enum LaunchMode {
    /**
     * A normal production build. At the moment this can be both native image or
     * JVM mode, but eventually these will likely be split
     */
    NORMAL(LaunchMode.PROD_PROFILE, "quarkus.profile"),
    /**
     * quarkus:dev or an IDE launch (when we support IDE launch)
     */
    DEVELOPMENT(LaunchMode.DEV_PROFILE, "quarkus.profile"),
    /**
     * a test run
     */
    TEST(LaunchMode.TEST_PROFILE, "quarkus.test.profile");

    public static final String DEV_PROFILE = "dev";
    public static final String PROD_PROFILE = "prod";
    public static final String TEST_PROFILE = "test";

    public boolean isDevOrTest() {
        return this != NORMAL;
    }

    public static boolean isDev() {
        return current() == DEVELOPMENT;
    }

    /**
     * Returns true if the current launch is the server side of remote dev.
     */
    public static boolean isRemoteDev() {
        return (current() == DEVELOPMENT) && "true".equals(System.getenv("QUARKUS_LAUNCH_DEVMODE"));
    }

    private final String defaultProfile;
    private final String profileKey;

    LaunchMode(final String defaultProfile, final String profileKey) {
        this.defaultProfile = defaultProfile;
        this.profileKey = profileKey;
    }

    public String getDefaultProfile() {
        return defaultProfile;
    }

    public String getProfileKey() {
        return profileKey;
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
