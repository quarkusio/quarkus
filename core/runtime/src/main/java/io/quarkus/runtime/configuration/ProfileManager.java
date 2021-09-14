package io.quarkus.runtime.configuration;

import io.quarkus.runtime.LaunchMode;

/**
 * Class that is responsible for resolving the current profile
 *
 * As this is needed immediately after startup it does not use any of the usual build/config infrastructure.
 *
 * The profile is resolved in the following way:
 *
 * <ul>
 * <li>The quarkus.profile system property</li>
 * <li>The QUARKUS_PROFILE environment entry</li>
 * <li>The default runtime profile provided during build</li>
 * <li>The default property for the launch mode</li>
 * </ul>
 *
 */
public class ProfileManager {

    public static final String QUARKUS_PROFILE_ENV = "QUARKUS_PROFILE";
    public static final String QUARKUS_PROFILE_PROP = "quarkus.profile";
    public static final String QUARKUS_TEST_PROFILE_PROP = "quarkus.test.profile";
    private static final String BACKWARD_COMPATIBLE_QUARKUS_PROFILE_PROP = "quarkus-profile";

    private static volatile LaunchMode launchMode = LaunchMode.NORMAL;
    private static volatile String runtimeDefaultProfile;

    public static void setLaunchMode(LaunchMode mode) {
        launchMode = mode;
    }

    public static LaunchMode getLaunchMode() {
        return launchMode;
    }

    public static void setRuntimeDefaultProfile(final String profile) {
        runtimeDefaultProfile = profile;
    }

    //NOTE: changes made here must be replicated in BootstrapProfile
    public static String getActiveProfile() {
        if (launchMode == LaunchMode.TEST) {
            String profile = System.getProperty(QUARKUS_TEST_PROFILE_PROP);
            if (profile != null) {
                return profile;
            }
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
