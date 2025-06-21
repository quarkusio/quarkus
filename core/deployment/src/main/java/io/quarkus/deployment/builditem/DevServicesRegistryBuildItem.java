package io.quarkus.deployment.builditem;

import java.io.Closeable;
import java.util.Set;
import java.util.UUID;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.crossclassloader.runtime.ComparableDevServicesConfig;
import io.quarkus.devservices.crossclassloader.runtime.DevServiceOwner;
import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesRegistry;
import io.quarkus.runtime.LaunchMode;

// Ideally we would have a unique build item for each processor/feature, but that would need a new KeyedBuildItem or FeatureBuildItem type
// Needs to be in core because DevServicesResultBuildItem is in core
public final class DevServicesRegistryBuildItem extends SimpleBuildItem {

    // This is a fairly thin wrapper around the tracker, so the tracker can be loaded with the system classloader
    // The QuarkusClassLoader takes care of loading the tracker with the right classloader
    private final RunningDevServicesRegistry tracker;
    private final UUID uuid;
    private final DevServicesConfig globalConfig;
    private final LaunchMode launchMode;

    public DevServicesRegistryBuildItem(UUID uuid, DevServicesConfig globalDevServicesConfig, LaunchMode launchMode) {
        this.launchMode = launchMode;
        this.tracker = new RunningDevServicesRegistry();
        this.uuid = uuid;
        this.globalConfig = globalDevServicesConfig;
    }

    public Set<Closeable> getRunningServices(String featureName, String configName, Object identifyingConfig) {
        DevServiceOwner owner = new DevServiceOwner(featureName, launchMode.name(), configName);
        ComparableDevServicesConfig key = new ComparableDevServicesConfig(uuid, owner, globalConfig, identifyingConfig);
        return tracker.getRunningServices(key);
    }

    public Set<Closeable> getAllRunningServices(String featureName, String configName) {
        DevServiceOwner owner = new DevServiceOwner(featureName, launchMode.name(), configName);
        return tracker.getAllRunningServices(owner);
    }

    public void addRunningService(String featureName, String configName, Object identifyingConfig,
            DevServicesResultBuildItem.RunnableDevService service) {
        DevServiceOwner owner = new DevServiceOwner(featureName, launchMode.name(), configName);
        ComparableDevServicesConfig key = new ComparableDevServicesConfig(uuid, owner, globalConfig, identifyingConfig);
        tracker.addRunningService(key, service);
    }

    public void removeRunningService(String featureName, String configName, Object identifyingConfig,
            DevServicesResultBuildItem.RunnableDevService service) {
        DevServiceOwner owner = new DevServiceOwner(featureName, launchMode.name(), configName);
        ComparableDevServicesConfig key = new ComparableDevServicesConfig(uuid, owner, globalConfig, identifyingConfig);
        tracker.removeRunningService(key, service);
    }

    public void closeAllRunningServices() {
        tracker.closeAllRunningServices(launchMode.name());
    }

}
