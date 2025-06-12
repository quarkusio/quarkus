package io.quarkus.devservices.crossclassloader.runtime;

import static java.util.UUID.randomUUID;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Note: This class should only use language-level classes and classes defined in this same package.
 * Other Quarkus classes might be in a different classloader.
 *
 * Warning: The methods in this class should *not* be called directly from an extension processor, in the augmentation phase.
 * Tracker-aware running dev services will only be registered post-augmentation, at runtime.
 */
public class RunningDevServicesTracker {

    // A useful uniqueness marker which will persist across profiles and application restarts, since this class lives in the system classloader. The value will be the same between dev and test mode.
    public static final String APPLICATION_UUID = randomUUID().toString();

    private static volatile Map<String, Set<Supplier<Map>>> configTracker = null;

    // A dev service owner is a combination of an extension (feature) and the app type (dev or test) which identifies which dev services
    // an extension processor can safely close.
    private static Map<DevServiceOwner, Set<Closeable>> servicesIndexedByOwner = null;
    private static Map<ComparableDevServicesConfig, Set<Closeable>> servicesIndexedByConfig = null;

    public RunningDevServicesTracker() {
        //This needs to work across classloaders, and the QuarkusClassLoader will load us parent first
        if (configTracker == null) {
            configTracker = new ConcurrentHashMap<>();
        }
        if (servicesIndexedByOwner == null) {
            servicesIndexedByOwner = new ConcurrentHashMap<>();
        }
        if (servicesIndexedByConfig == null) {
            servicesIndexedByConfig = new ConcurrentHashMap<>();
        }
    }

    /**
     *
     * @return may return null
     */
    public Set<Supplier<Map>> getConfigForAllRunningServices(String launchMode) {
        // This gets called an awful lot. Should we cache it? If we did, we'd need to deal with cache invalidation, so maybe not.
        return configTracker.get(launchMode);
    }

    public Set<Closeable> getRunningServices(ComparableDevServicesConfig identifyingConfig) {
        return servicesIndexedByConfig.get(identifyingConfig);
    }

    public Set<Closeable> getAllRunningServices(DevServiceOwner owner) {
        return servicesIndexedByOwner.get(owner);
    }

    public void addRunningService(ComparableDevServicesConfig key, Closeable service) {
        addServiceToIndex(servicesIndexedByOwner, key.getDevServicesOwner(), service);
        addServiceToIndex(servicesIndexedByConfig, key, service);
        addServiceToConfig(key.getDevServicesOwner().launchMode(), (Supplier<Map>) service);
    }

    // The service passed in here might be from a different classloader
    public void removeRunningService(ComparableDevServicesConfig key, Closeable service) {
        DevServiceOwner owner = key.getDevServicesOwner();
        removeServiceFromIndex(servicesIndexedByConfig, key, service);
        removeServiceFromIndex(servicesIndexedByOwner, owner, service);
        removeServiceFromConfig(owner.launchMode(), (Supplier<Map>) service);
    }

    static <T, K> void addServiceToIndex(Map<K, Set<T>> servicesIndexed, K key, T value) {
        servicesIndexed.computeIfAbsent(key, k -> new HashSet<>()).add(value);
    }

    static <T, K> void removeServiceFromIndex(Map<K, Set<T>> servicesIndexed, K key, T value) {
        Set<T> servicesForOwner = servicesIndexed.get(key);
        if (servicesForOwner != null) {
            servicesForOwner.remove(value);
            if (servicesForOwner.isEmpty()) {
                servicesIndexed.remove(key);
            }
        }
    }

    void addServiceToConfig(String launchMode, Supplier<Map> configSupplier) {
        configTracker.computeIfAbsent(launchMode, k -> new HashSet<>()).add(configSupplier);
    }

    void removeServiceFromConfig(String launchMode, Supplier<Map> configSupplier) {
        Set<Supplier<Map>> configs = configTracker.get(launchMode);
        if (configs != null) {
            configs.remove(configSupplier);
            if (configs.isEmpty()) {
                configTracker.remove(launchMode);
            }
        }
    }
}
