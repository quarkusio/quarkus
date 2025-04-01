package io.quarkus.devservices.crossclassloader.runtime;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * * Note: This class should only use language-level classes and classes defined in this same package.
 * Other Quarkus classes might be in a different classloader.
 */
public class RunningDevServicesTracker {
    private static volatile Set<Supplier<Map>> configTracker = null;

    private static Map<String, DevServicesIndexedByConfig> servicesIndexedByFeature = null;

    public RunningDevServicesTracker() {
        //This needs to work across classloaders, but the QuarkusClassLoader will load us parent first
        if (configTracker == null) {
            configTracker = new HashSet<>();
        }
        if (servicesIndexedByFeature == null) {
            servicesIndexedByFeature = new HashMap<>();
        }
    }

    // This gets called an awful lot. Should we cache it?
    public Set<Supplier<Map>> getConfigForAllRunningServices() {
        return Collections.unmodifiableSet(configTracker);
    }

    public List<Closeable> getRunningServices(String featureName,
            Map identifyingConfig) {
        DevServicesIndexedByConfig services = servicesIndexedByFeature.get(featureName);
        if (services != null) {
            return services.get(identifyingConfig);
        }
        return null;
    }

    public Set<Closeable> getAllServices(String featureName) {
        DevServicesIndexedByConfig services = servicesIndexedByFeature.get(featureName);
        if (services == null) {
            return Set.of();
        } else {
            // Flatten
            Set<Closeable> ls = new HashSet<>();
            services.values().forEach(ls::addAll);
            return ls;
        }
    }

    public void addRunningService(String name, Map<String, String> identifyingConfig,
            Closeable service) {
        DevServicesIndexedByConfig services = servicesIndexedByFeature.get(name);

        if (services == null) {
            services = new DevServicesIndexedByConfig();
            servicesIndexedByFeature.put(name, services);
        }

        // Make a list so that we can add and remove to it
        List<Closeable> list = new ArrayList<>();
        list.add(service);
        services.put(identifyingConfig, list);

        configTracker.add((Supplier<Map>) service);
    }

    public void removeRunningService(String name, Map<String, String> identifyingConfig,
            Closeable service) {
        DevServicesIndexedByConfig services = servicesIndexedByFeature.get(name);

        if (services != null) {
            List servicesForConfig = services.get(identifyingConfig);
            if (servicesForConfig != null) {
                servicesForConfig.remove(service);
            }
        }

        configTracker.remove(service);
    }

    /**
     * Type to give a bit of clarity of intent and avoid some of the thicket of angle brackets.
     * The key is a map of identifying config, and the value is a List of RunningDevService objects ... only they might be in a
     * different classloader, so we don't call them that.
     */
    private static class DevServicesIndexedByConfig extends HashMap<Map, List<Closeable>> {
        public DevServicesIndexedByConfig() {
            super();
        }
    }
}
