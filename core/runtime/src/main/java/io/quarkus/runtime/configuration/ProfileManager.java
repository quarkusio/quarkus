package io.quarkus.runtime.configuration;

import io.quarkus.runtime.LaunchMode;

/**
 * Class that is responsible for resolving the current profile
 *
 * As this is needed immediately after startup it does not use any of the usual build/config infrastructure.
 *
 * The profile is resolved in the following way:
 *
 * - The quarkus.profile system property
 * - The QUARKUS_PROFILE environment entry
 * - The default runtime profile provided during build
 * - The default property for the launch mode
 *
 */
public class ProfileManager {

    public static final String QUARKUS_PROFILE_ENV = "QUARKUS_PROFILE";
    public static final String QUARKUS_PROFILE_PROP = "quarkus.profile";
    private static final String BACKWARD_COMPATIBLE_QUARKUS_PROFILE_PROP = "quarkus-profile";

    private static volatile LaunchMode launchMode = LaunchMode.NORMAL;
    private static String runtimeDefaultProfile = null;

    public static void setLaunchMode(LaunchMode mode) {
        launchMode = mode;
    }

    public static void setRuntimeDefaultProfile(final String profile) {
        runtimeDefaultProfile = profile;
    }

    public static String getActiveProfile() {
        if (launchMode == LaunchMode.TEST) {
            return launchMode.getDefaultProfile();
        }

        String profile = System.getProperty(QUARKUS_PROFILE_PROP);
        if (profile != null) {
            return profile;
        }

        profile = System.getProperty(BACKWARD_COMPATIBLE_QUARKUS_PROFILE_PROP);
        if (profile != null) {
            return profile;
        }

        profile = System.getenv(QUARKUS_PROFILE_ENV);
        if (profile != null) {
            return profile;
        }

        profile = runtimeDefaultProfile;
        if (profile != null) {
            return profile;
        }

        return launchMode.getDefaultProfile();
    }

}
