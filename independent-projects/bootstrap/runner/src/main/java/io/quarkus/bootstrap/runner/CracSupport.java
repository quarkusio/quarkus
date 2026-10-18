package io.quarkus.bootstrap.runner;

public final class CracSupport {

    private static final boolean ENABLED = Boolean.getBoolean("quarkus.package.jar.crac.enabled");

    private CracSupport() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }
}
