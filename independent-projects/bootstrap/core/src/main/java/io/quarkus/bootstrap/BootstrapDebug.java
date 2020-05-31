package io.quarkus.bootstrap;

public final class BootstrapDebug {

    public static final String DEBUG_SOURCES_DIR = System.getProperty("quarkus.debug.generated-sources-dir");

    public static final String DEBUG_CLASSES_DIR = System.getProperty("quarkus.debug.generated-classes-dir");

    public static final String DEBUG_TRANSFORMED_CLASSES_DIR = System.getProperty("quarkus.debug.transformed-classes-dir");

    private BootstrapDebug() {
    }

}
