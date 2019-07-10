package io.quarkus.runtime.configuration;

import io.quarkus.runtime.LaunchMode;

/**
 * Class that is responsible for resolving the current profile
 *
 * As this is needed immediately after startup it does not use any of the usual build/config infrastructure.
 *
 * The profile is resolved in the following way:
 *
 * - The QUARKUS_PROFILE environment entry
 * - The quarkus-profile system property
 * - The default property for the launch mode
 *
 */
public class ProfileManager {

    public static final String QUARKUS_PROFILE_ENV = "QUARKUS_PROFILE";
    public static final String QUARKUS_PROFILE_PROP = "quarkus-profile";

    private static volatile LaunchMode launchMode = LaunchMode.NORMAL;

    public static void setLaunchMode(LaunchMode mode) {
        launchMode = mode;
    }

    public static String getActiveProfile() {
        return getActiveProfile(null);
    }

    public static String getActiveProfile(String buildProfile) {
        String profile = System.getenv(QUARKUS_PROFILE_ENV);
        if (profile != null) {
            return profile;
        }
        profile = System.getProperty(QUARKUS_PROFILE_PROP);
        if (profile != null) {
            return profile;
        }
        if (buildProfile != null) {
            return buildProfile;
        }
        return launchMode.getDefaultProfile();
    }

}
