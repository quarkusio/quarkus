package io.quarkus.deployment.builditem;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
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

    public List getRunningServices(String featureName,
            Map identifyingConfig) {
        return tracker.getRunningServices(featureName, identifyingConfig);
    }

    public Set<Closeable> getAllServices(String featureName) {
        return tracker.getAllServices(featureName);
    }

    public void addRunningService(String name, Map<String, String> identifyingConfig,
            DevServicesResultBuildItem.RunnableDevService service) {
        tracker.addRunningService(name, identifyingConfig, service);
    }

    public void removeRunningService(String name, Map<String, String> identifyingConfig,
            DevServicesResultBuildItem.RunnableDevService service) {
        tracker.removeRunningService(name, identifyingConfig, service);
    }

}
