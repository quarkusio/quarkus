package io.quarkus.bootstrap.app;

/**
 * Mirror of the logic in ProfileManager, but this is needed pre-bootstrap so there is nowhere to really share it.
 *
 * This is only used for reading the class loading config
 */
public class BootstrapProfile {

    public static final String QUARKUS_PROFILE_ENV = "QUARKUS_PROFILE";
    public static final String QUARKUS_PROFILE_PROP = "quarkus.profile";
    public static final String QUARKUS_TEST_PROFILE_PROP = "quarkus.test.profile";
    private static final String BACKWARD_COMPATIBLE_QUARKUS_PROFILE_PROP = "quarkus-profile";
    public static final String DEV = "dev";
    public static final String PROD = "prod";
    public static final String TEST = "test";

    private static String runtimeDefaultProfile = null;

    public static void setRuntimeDefaultProfile(final String profile) {
        runtimeDefaultProfile = profile;
    }

    public static String getActiveProfile(QuarkusBootstrap.Mode mode) {
        if (mode == QuarkusBootstrap.Mode.TEST) {
            String profile = System.getProperty(QUARKUS_TEST_PROFILE_PROP);
            if (profile != null) {
                return profile;
            }
            return "test";
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
        switch (mode) {
            case REMOTE_DEV_SERVER:
            case DEV:
            case CONTINUOUS_TEST:
                return DEV;
            case REMOTE_DEV_CLIENT:
            case PROD:
                return PROD;
            default:
                throw new RuntimeException("unknown mode:" + mode);
        }
    }
}
