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
    private static volatile LaunchMode launchMode = LaunchMode.NORMAL;

    public static void setLaunchMode(LaunchMode mode) {
        launchMode = mode;
    }

    public static LaunchMode getLaunchMode() {
        return launchMode;
    }

    //NOTE: changes made here must be replicated in BootstrapProfile

}
