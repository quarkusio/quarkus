package io.quarkus.deployment.builditem;

import java.io.Closeable;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.crossclassloader.runtime.ComparableDevServicesConfig;
import io.quarkus.devservices.crossclassloader.runtime.DevServiceOwner;
import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesTracker;

// Ideally we would have a unique build item for each processor/feature, but that would need a new KeyedBuildItem or FeatureBuildItem type
// Needs to be in core because DevServicesResultBuildItem is in core
public final class DevServicesTrackerBuildItem extends SimpleBuildItem {

    // This is a fairly thin wrapper around the tracker, so the tracker can be loaded with the system classloader
    // The QuarkusClassLoader takes care of loading the tracker with the right classloader
    private final RunningDevServicesTracker tracker;

    public DevServicesTrackerBuildItem() {
        tracker = new RunningDevServicesTracker();
    }

    public Set<Closeable> getRunningServices(DevServiceOwner owner, DevServicesConfig globalConfig,
            Object identifyingConfig) {
        ComparableDevServicesConfig key = new ComparableDevServicesConfig(owner, globalConfig, identifyingConfig);
        return tracker.getRunningServices(key);
    }

    public Set<Closeable> getAllRunningServices(DevServiceOwner owner) {
        return tracker.getAllRunningServices(owner);
    }

    public void addRunningService(DevServiceOwner owner, DevServicesConfig globalConfig,
            Object identifyingConfig,
            DevServicesResultBuildItem.RunnableDevService service) {
        ComparableDevServicesConfig key = new ComparableDevServicesConfig(owner, globalConfig, identifyingConfig);
        tracker.addRunningService(key, service);
    }

    public void removeRunningService(DevServiceOwner owner, DevServicesConfig globalConfig,
            Object identifyingConfig,
            DevServicesResultBuildItem.RunnableDevService service) {
        ComparableDevServicesConfig key = new ComparableDevServicesConfig(owner, globalConfig, identifyingConfig);
        tracker.removeRunningService(key, service);
    }

}
