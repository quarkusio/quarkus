package io.quarkus.bootstrap.app;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

public interface StartupAction {

    static Object getTestClassLocation() {
        return StartupActionHolder.getTestClassLocation();
    }

    static void storeTestClassLocation(Path testClassLocation) {
        StartupActionHolder.setTestClassLocation(testClassLocation);
    }

    void store();

    /**
     * Overrides runtime config.
     */
    void overrideConfig(Map<String, String> config);

    RunningQuarkusApplication run(String... args) throws Exception;

    ClassLoader getClassLoader();

    Map<String, String> getDevServicesProperties();

    /**
     * Runs the application by running the main method of the main class. As this is a blocking method a new
     * thread is created to run this task.
     *
     * Before this method is called an appropriate exit handler will likely need to
     * be set in {@link io.quarkus.runtime.ApplicationLifecycleManager#setDefaultExitCodeHandler(Consumer)}
     * of the JVM will exit when the app stops.
     */
    RunningQuarkusApplication runMainClass(String... args) throws Exception;

    /**
     * Runs the application by running the main method of the main class, and runs it to completion
     */
    int runMainClassBlocking(String... args) throws Exception;

    static StartupAction getStored() {
        return StartupActionHolder.getStored();
    }

}
