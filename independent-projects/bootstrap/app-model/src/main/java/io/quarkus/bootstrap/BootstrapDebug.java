package io.quarkus.bootstrap;

public final class BootstrapDebug {

    // We're exposing the configuration properties as methods and not constants,
    // because in the case of tests, the system property could change over the life of the JVM.

    public static String debugClassesDir() {
        return System.getProperty("quarkus.debug.generated-classes-dir");
    }

    public static String transformedClassesDir() {
        return System.getProperty("quarkus.debug.transformed-classes-dir");
    }

    public static String debugSourcesDir() {
        return System.getProperty("quarkus.debug.generated-sources-dir");
    }

    private BootstrapDebug() {
    }

}
