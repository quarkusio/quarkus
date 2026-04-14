package io.quarkus.devshell.runtime;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Recorder for Dev Shell runtime initialization.
 * This class must be in the runtime module because it's used by the build system.
 * The actual TUI launching is delegated to DevShellLauncherRuntime in runtime-dev.
 */
@Recorder
public class DevShellRecorder {

    private static final Logger LOG = Logger.getLogger(DevShellRecorder.class);

    public static final String LAUNCHER_KEY = "devshell-launcher";
    public static final String EXTENSIONS_KEY = "devshell-extensions";
    public static final String SHELL_PAGES_KEY = "devshell-pages";
    public static final String ROUTER_KEY = "devshell-router";
    public static final String BUILD_TIME_DATA_KEY = "devshell-build-time-data";
    public static final String BUILD_TIME_DATA_READER_KEY = "devshell-build-time-data-reader";
    public static final String RUNTIME_CL_KEY = "devshell-runtime-cl";
    public static final String BEAN_RESOLVER_KEY = "devshell-bean-resolver";

    /**
     * Called at RUNTIME_INIT to set up the launcher.
     * This runs in the runtime classloader where Arc is available.
     */
    public void initializeRouter(BeanContainer beanContainer) {
        DevShellRouter router = beanContainer.beanInstance(DevShellRouter.class);
        // Store the router in DevConsoleManager so runtime-dev can access it
        DevConsoleManager.setGlobal(ROUTER_KEY, router);

        // Also store the BuildTimeDataReader
        BuildTimeDataReader dataReader = beanContainer.beanInstance(BuildTimeDataReader.class);
        DevConsoleManager.setGlobal(BUILD_TIME_DATA_READER_KEY, dataReader);

        // Store the runtime classloader so the TUI thread can use it
        ClassLoader runtimeCL = Thread.currentThread().getContextClassLoader();
        DevConsoleManager.setGlobal(RUNTIME_CL_KEY, runtimeCL);

        // Store a bean resolver function that uses the runtime classloader's Arc
        // This lambda captures the runtime CL context, so Arc.container() works
        Function<String, Object> beanResolver = className -> {
            try {
                Class<?> clazz = runtimeCL.loadClass(className);
                InstanceHandle<?> handle = Arc.container().instance(clazz);
                return handle.isAvailable() ? handle.get() : null;
            } catch (Exception e) {
                LOG.debugf(e, "Failed to resolve bean: %s", className);
                return null;
            }
        };
        DevConsoleManager.setGlobal(BEAN_RESOLVER_KEY, beanResolver);
    }

    /**
     * Called at RUNTIME_INIT to set the extension data.
     * Converts to primitive arrays for cross-classloader compatibility.
     */
    public void setExtensions(List<String[]> extensionData) {
        DevConsoleManager.setGlobal(EXTENSIONS_KEY, extensionData);
    }

    /**
     * Called at RUNTIME_INIT to set shell page provider information.
     * Converts to primitive arrays for cross-classloader compatibility.
     */
    public void setShellPages(Map<String, String[]> pageData) {
        LOG.debugf("setShellPages called with %d pages", pageData != null ? pageData.size() : 0);
        DevConsoleManager.setGlobal(SHELL_PAGES_KEY, pageData);
        LOG.debugf("Shell pages stored in DevConsoleManager");
    }

    /**
     * Called at RUNTIME_INIT to set build-time data from extensions.
     * Data is passed as JSON strings keyed by extension namespace.
     */
    public void setBuildTimeData(Map<String, Map<String, String>> buildTimeData) {
        LOG.debugf("setBuildTimeData called with %d extensions", buildTimeData != null ? buildTimeData.size() : 0);
        DevConsoleManager.setGlobal(BUILD_TIME_DATA_KEY, buildTimeData);
    }

    /**
     * Trigger the TUI launch.
     * Called from deployment after setting up the communication queues.
     */
    @SuppressWarnings("unchecked")
    public static void triggerLaunch() {
        Consumer<Object> launcher = DevConsoleManager.getGlobal(LAUNCHER_KEY);
        if (launcher == null) {
            System.err.println("Failed to start Dev Shell: Launcher is not initialized");
            return;
        }
        launcher.accept(null);
    }
}
