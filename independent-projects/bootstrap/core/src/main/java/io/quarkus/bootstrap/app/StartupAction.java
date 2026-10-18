package io.quarkus.bootstrap.app;

import java.io.Closeable;
import java.util.Map;
import java.util.function.Consumer;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

public interface StartupAction {

    /**
     * Overrides runtime config.
     */
    void overrideConfig(Map<String, String> config);

    RunningQuarkusApplication run(String... args) throws Exception;

    QuarkusClassLoader getClassLoader();

    /**
     * This will start any dev services that were not started in the augmentation phase.
     */
    Map<String, String> getOrInitialiseDevServicesProperties();

    /**
     * This will start any dev services that were not started in the augmentation phase.
     */
    String getOrInitialiseDevServicesNetworkId();

    /**
     * Runs the application by running the main method of the main class. As this is a blocking method a new
     * thread is created to run this task.
     * <p>
     * Before this method is called an appropriate exit handler will likely need to
     * be set in {@link io.quarkus.runtime.ApplicationLifecycleManager#setDefaultExitCodeHandler(Consumer)}
     * of the JVM will exit when the app stops.
     */
    RunningQuarkusApplication runMainClass(String... args) throws Exception;

    /**
     * Runs the application by running the main method of the main class, and runs it to completion
     */
    int runMainClassBlocking(String... args) throws Exception;

    void addRuntimeCloseTask(Closeable closeTask);

    /**
     * This particular StartupAction might need to enforce Java module configurations as
     * required by the extensions. Such tuning often need to be applied to the running
     * classloader: if additional classloaders are derived from the application classloader,
     * they should be adjusted accordingly by invoking this method on them before starting
     * the application. (This is apparently necessary in the JUnit integration).
     *
     * @param classLoader
     */
    void applyModuleConfigurationToClassloader(ClassLoader classLoader);

}
