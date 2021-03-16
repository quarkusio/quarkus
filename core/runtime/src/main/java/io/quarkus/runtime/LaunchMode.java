package io.quarkus.runtime;

import io.quarkus.runtime.configuration.ProfileManager;

public enum LaunchMode {

    /**
     * A normal production build. At the moment this can be both native image or
     * JVM mode, but eventually these will likely be split
     */
    NORMAL("prod"),
    /**
     * quarkus:dev or an IDE launch (when we support IDE launch)
     */
    DEVELOPMENT("dev"),
    /**
     * a test run
     */
    TEST("test");

    public boolean isDevOrTest() {
        return this != NORMAL;
    }

    /**
     * Returns true if the current launch is the server side of remote dev.
     */
    public static boolean isRemoteDev() {
        return (current() == DEVELOPMENT) && "true".equals(System.getenv("QUARKUS_LAUNCH_DEVMODE"));
    }

    private final String defaultProfile;

    LaunchMode(String defaultProfile) {
        this.defaultProfile = defaultProfile;
    }

    public String getDefaultProfile() {
        return defaultProfile;
    }

    /**
     *
     * @return The current launch mode
     */
    public static LaunchMode current() {
        return ProfileManager.getLaunchMode();
    }
}
