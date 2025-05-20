package io.quarkus.devservices.crossclassloader.runtime;

import java.io.Closeable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Note: This class should only use language-level classes and classes defined in this same package.
 * Other Quarkus classes might be in a different classloader.
 *
 * Warning: The methods in this class should *not* be called directly from an extension processor, in the augmentation phase.
 * Tracker-aware running dev services will only be registered post-augmentation, at runtime.
 */
public class RunningDevServicesTracker {
    private static volatile Set<Supplier<Map>> configTracker = null;

    // A dev service owner is a combination of an extension (feature) and the app type (dev or test) which identifies which dev services
    // an extension processor can safely close.
    private static Map<DevServiceOwner, Set<Closeable>> servicesIndexedByOwner = null;
    private static Map<ComparableDevServicesConfig, Set<Closeable>> servicesIndexedByConfig = null;

    public RunningDevServicesTracker() {
        //This needs to work across classloaders, but the QuarkusClassLoader will load us parent first
        if (configTracker == null) {
            configTracker = new HashSet<>();
        }
        if (servicesIndexedByOwner == null) {
            servicesIndexedByOwner = new HashMap<>();
        }
        if (servicesIndexedByConfig == null) {
            servicesIndexedByConfig = new HashMap<>();
        }
    }

    // This gets called an awful lot. Should we cache it?
    public Set<Supplier<Map>> getConfigForAllRunningServices() {
        return Collections.unmodifiableSet(configTracker);
    }

    public Set<Closeable> getRunningServices(ComparableDevServicesConfig identifyingConfig) {
        return servicesIndexedByConfig.get(identifyingConfig);
    }

    public Set<Closeable> getAllRunningServices(DevServiceOwner owner) {
        return servicesIndexedByOwner.get(owner);
    }

    public void addRunningService(ComparableDevServicesConfig key,
            Closeable service) {
        {
            DevServiceOwner owner = key.getDevServicesOwner();
            Set<Closeable> services = servicesIndexedByOwner.get(owner);

            if (services == null) {
                // Make a Set so that we can add and remove to it
                services = new HashSet();
                servicesIndexedByOwner.put(owner, services);
            }

            services.add(service);
        }
        {
            Set<Closeable> services = servicesIndexedByConfig.get(key);
            if (services == null) {
                // Make a Set so that we can add and remove to it
                services = new HashSet();
                servicesIndexedByConfig.put(key, services);
                services.add(service);
            }
        }

        configTracker.add((Supplier<Map>) service);
    }

    // The service passed in here might be from a different classloader
    public void removeRunningService(ComparableDevServicesConfig key,
            Closeable service) {
        DevServiceOwner owner = key.getDevServicesOwner();

        {
            Set servicesForConfig = servicesIndexedByConfig.get(key);
            if (servicesForConfig != null) {
                servicesForConfig.remove(service);
            }
        }

        {
            Set servicesForOwner = servicesIndexedByOwner.get(owner);
            if (servicesForOwner != null) {
                servicesForOwner.remove(service);
            }
        }

        configTracker.remove(service);
    }
}
