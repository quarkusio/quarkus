package io.quarkus.bootstrap;

public final class BootstrapDebug {

    /**
     * @deprecated Use {@link #debugClassesDir()} instead for more flexibility when testing.
     */
    @Deprecated
    public static final String DEBUG_CLASSES_DIR = System.getProperty("quarkus.debug.generated-classes-dir");

    /**
     * @deprecated Use {@link #transformedClassesDir()} instead for more flexibility when testing.
     */
    @Deprecated
    public static final String DEBUG_TRANSFORMED_CLASSES_DIR = System.getProperty("quarkus.debug.transformed-classes-dir");

    /**
     * @deprecated Use {@link #debugSourcesDir()} instead for more flexibility when testing.
     */
    @Deprecated
    public static final String DEBUG_SOURCES_DIR = System.getProperty("quarkus.debug.generated-sources-dir");

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
