package io.quarkus.runtime.configuration;

import static io.smallrye.config.Converters.newCollectionConverter;
import static io.smallrye.config.Converters.newEmptyValueConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.Converter;

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
 */
public class ProfileManager {

    public static final String QUARKUS_PROFILE_ENV = "QUARKUS_PROFILE";
    public static final String QUARKUS_PROFILE_PROP = "quarkus.profile";
    public static final String QUARKUS_TEST_PROFILE_PROP = "quarkus.test.profile";
    private static final String BACKWARD_COMPATIBLE_QUARKUS_PROFILE_PROP = "quarkus-profile";

    private static volatile LaunchMode launchMode = LaunchMode.NORMAL;

    // This one is used to record the build profile, so when running native, the runtime uses the same profile and
    // not default to prod. See https://github.com/quarkusio/quarkus/issues/3147
    private static String runtimeDefaultProfile = null;

    public static void setLaunchMode(LaunchMode mode) {
        launchMode = mode;
    }

    public static LaunchMode getLaunchMode() {
        return launchMode;
    }

    public static void setRuntimeDefaultProfile(final String profile) {
        runtimeDefaultProfile = profile;
    }

    /**
     * Return the active profile. If multiple profiles are active, the returned profile is a single {@code String} with
     * each profile name separated with a comma @{code ","}.
     *
     * @deprecated this method does not properly return the active profile, since multiple profile may be active.
     *             Prefer to use {@link #getActiveProfiles()}.
     *
     * @see #getActiveProfiles()
     * @return the name of the active profile.
     */
    @Deprecated
    public static String getActiveProfile() {
        return String.join(",", getActiveProfiles());
    }

    /**
     * Lookups the profile or profiles to use from an accepted list of environment variables or system properties.
     * <p>
     * Multiple profiles may be set in any of these sources, by separating each profile with a comma @{code ","}.
     *
     * @see #QUARKUS_PROFILE_ENV
     * @see #QUARKUS_PROFILE_PROP
     * @see #QUARKUS_TEST_PROFILE_PROP
     * @see #BACKWARD_COMPATIBLE_QUARKUS_PROFILE_PROP
     * @return the profile configuration.
     */
    public static String getProfileConfiguration() {
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

    /**
     * Returns the active list of profiles.
     *
     * @return a list of names with all the active profiles.
     */
    public static List<String> getActiveProfiles() {
        List<String> profiles = PROFILE_CONVERTER.convert(getProfileConfiguration());
        Collections.reverse(profiles);
        return profiles;
    }

    public static boolean isProfileActive(final String profile) {
        return getActiveProfiles().contains(profile);
    }

    private static final Converter<List<String>> PROFILE_CONVERTER = newCollectionConverter(
            newEmptyValueConverter(value -> value), ArrayList::new);
}
