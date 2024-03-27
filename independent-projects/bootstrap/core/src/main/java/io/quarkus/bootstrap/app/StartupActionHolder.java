package io.quarkus.bootstrap.app;

import java.nio.file.Path;

// TODO find a cleaner pattern for this
public class StartupActionHolder {
    static StartupAction stored = null;
    static Path testClassLocation = null;

    public static void store(StartupAction startupAction) {
        stored = startupAction;
    }

    public static StartupAction getStored() {
        return stored;
    }

    public static Path getTestClassLocation() {
        return testClassLocation;
    }

    public static void setTestClassLocation(Path path) {
        testClassLocation = path;
    }
}
