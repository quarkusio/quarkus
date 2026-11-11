package io.quarkus.bootstrap.runner;

/**
 * Tells whether the application registers its CRaC resources.
 */
public final class CracSupport {

    /**
     * The name of the build time property that enables the CRaC support. The build sets the system property of the same
     * name when the build time property is {@code true}.
     */
    public static final String ENABLED_PROPERTY = "quarkus.package.jar.crac.enabled";

    private CracSupport() {
    }

    /**
     * The value is not cached, because the application sets the system property after this class is initialized.
     *
     * @return {@code true} if the system property {@value #ENABLED_PROPERTY} is {@code true}
     */
    public static boolean isEnabled() {
        return Boolean.getBoolean(ENABLED_PROPERTY);
    }
}
