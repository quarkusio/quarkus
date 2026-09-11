package io.quarkus.runtime;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

public class StartupContext implements Closeable {

    public static final String RAW_COMMAND_LINE_ARGS = StartupContext.class.getName() + ".raw-command-line-args";

    private static final Logger LOG = Logger.getLogger(StartupContext.class);

    // Holds values for returned proxies
    // These values are usually returned from recorder methods but can be also set explicitly
    // For example, the raw command line args and ShutdownContext are set when the StartupContext is created
    // Concurrent: legacy recorder nodes may run in parallel on the service graph executor
    private final Map<String, Object> values = new ConcurrentHashMap<>();

    /*
     * Service value storage exists for compatibility with the bytecode recorder execution model,
     * which clears the main values map between static-init and runtime-init phases. Service values
     * use a separate map so they can selectively survive this clearing. Once bytecode recorders
     * are removed, service value storage will be replaced with a direct value-passing mechanism
     * that does not require a map.
     * Concurrent: service nodes may run in parallel on the service graph executor.
     */
    private final Map<String, Object> serviceValues = new ConcurrentHashMap<>();

    private String[] commandLineArgs;
    private String currentBuildStepName;

    public StartupContext() {
        values.put(RAW_COMMAND_LINE_ARGS, new Supplier<String[]>() {
            @Override
            public String[] get() {
                if (commandLineArgs == null) {
                    throw new RuntimeException("Command line arguments not available during static init");
                }
                return commandLineArgs;
            }
        });
    }

    public void putValue(String name, Object value) {
        if (value != null) {
            values.put(name, value);
        }
    }

    public Object getValue(String name) {
        return values.get(name);
    }

    /**
     * Store a service value produced by a service action.
     *
     * @param name the service key (must not be {@code null})
     * @param value the service value (must not be {@code null})
     */
    public void putServiceValue(String name, Object value) {
        if (value != null) {
            serviceValues.put(name, value);
        }
    }

    /**
     * Retrieve a service value.
     *
     * @param name the service key
     * @return the service value, or {@code null} if not found
     */
    public Object getServiceValue(String name) {
        return serviceValues.get(name);
    }

    /**
     * Remove and return a service value.
     * Used by CDI service-value beans to drain the map after lazy instantiation.
     *
     * @param name the service key
     * @return the previous value, or {@code null} if not found
     */
    public Object removeServiceValue(String name) {
        return serviceValues.remove(name);
    }

    /**
     * Retain only the specified service value keys, removing all others.
     * Called between static-init and runtime-init to discard static-init-only
     * service values while keeping those needed by runtime-init services.
     *
     * @param keys the service keys to retain
     */
    public void retainServiceValues(Set<String> keys) {
        serviceValues.keySet().retainAll(keys);
    }

    /**
     * Clear all service values. Called after all deploy methods complete.
     */
    public void clearServiceValues() {
        serviceValues.clear();
    }

    @Override
    public void close() {
        values.clear();
        serviceValues.clear();
    }

    @SuppressWarnings("unused")
    public void setCommandLineArguments(String[] commandLineArguments) {
        this.commandLineArgs = commandLineArguments;
    }

    @SuppressWarnings("unused")
    public String getCurrentBuildStepName() {
        return currentBuildStepName;
    }

    @SuppressWarnings("unused")
    public void setCurrentBuildStepName(String currentBuildStepName) {
        this.currentBuildStepName = currentBuildStepName;
    }
}
