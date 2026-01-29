package io.quarkus.devservices.crossclassloader.runtime;

import static java.util.UUID.randomUUID;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

/**
 * Note: This class should only use language-level classes and classes defined in this same package.
 * Other Quarkus classes might be in a different classloader.
 * <p>
 * Warning: The methods in this class should *not* be called directly from an extension processor, in the augmentation phase.
 * Tracker-aware running dev services will only be registered post-augmentation, at runtime.
 */
public final class RunningDevServicesRegistry {

    private static final Logger log = Logger.getLogger(RunningDevServicesRegistry.class);

    public static final RunningDevServicesRegistry INSTANCE = new RunningDevServicesRegistry();

    // A useful uniqueness marker which will persist across profiles and application restarts, since this class lives in the system classloader. The value will be the same between dev and test mode.
    public static final String APPLICATION_UUID = randomUUID().toString();

    // This index is needed for the DevServicesConfigSource to be able to access the dev services running in a specific launch mode.
    private static final Map<String, Set<RunningService>> servicesIndexedByLaunchMode = new ConcurrentHashMap<>();

    // A dev service owner is a combination of an extension (feature) and the app type (dev or test) which identifies which dev services
    // an extension processor can safely close.
    private final Map<ComparableDevServicesConfig, RunningService> servicesIndexedByConfig = new ConcurrentHashMap<>();

    private RunningDevServicesRegistry() {
    }

    void logClosing(String featureName, String launchMode, String containerId) {
        log.debugf("Closing dev service for %s in launch mode %s: %s", featureName, launchMode, containerId);
    }

    void logFailedToClose(Exception e, String featureName, String launchMode, String containerId) {
        log.infof(e, "Failed to close dev service for %s in launch mode %s: %s", featureName, launchMode, containerId);
    }

    public void closeAllRunningServices(String launchMode) {
        Iterator<Map.Entry<ComparableDevServicesConfig, RunningService>> it = servicesIndexedByConfig.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ComparableDevServicesConfig, RunningService> next = it.next();
            DevServiceOwner owner = next.getKey().owner();
            if (owner.launchMode().equals(launchMode)) {
                it.remove();
                RunningService service = next.getValue();
                try {
                    logClosing(owner.featureName(), launchMode, service.containerId());
                    service.close();
                } catch (Exception e) {
                    // We don't want to fail the shutdown hook if a service fails to close
                    logFailedToClose(e, owner.featureName(), launchMode, service.containerId());
                }
            }
        }
        servicesIndexedByLaunchMode.remove(launchMode);
    }

    public void closeRemainingRunningServices(UUID uuid, String launchMode, Collection<DevServiceOwner> ownersToKeep) {
        Set<RunningService> services = servicesIndexedByLaunchMode.get(launchMode);
        var iterator = servicesIndexedByConfig.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ComparableDevServicesConfig, RunningService> entry = iterator.next();
            DevServiceOwner owner = entry.getKey().owner();
            UUID serviceAppUuid = entry.getKey().applicationInstanceId();
            if (owner.launchMode().equals(launchMode) && Objects.equals(serviceAppUuid, uuid)
                    && !ownersToKeep.contains(owner)) {
                iterator.remove();
                RunningService service = entry.getValue();
                services.remove(service);
                try {
                    logClosing(owner.featureName(), launchMode, service.containerId());
                    service.close();
                } catch (Exception e) {
                    // We don't want to fail the shutdown hook if a service fails to close
                    logFailedToClose(e, owner.featureName(), launchMode, service.containerId());
                }
            }
        }

    }

    public void closeAllRunningServices(DevServiceOwner owner) {
        Set<RunningService> launchModeServices = servicesIndexedByLaunchMode.get(owner.launchMode());
        var iterator = servicesIndexedByConfig.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ComparableDevServicesConfig, RunningService> entry = iterator.next();
            DevServiceOwner entryOwner = entry.getKey().owner();
            if (Objects.equals(entryOwner, owner)) {
                iterator.remove();
                RunningService service = entry.getValue();
                if (launchModeServices != null) {
                    launchModeServices.remove(service);
                }
                try {
                    logClosing(owner.featureName(), owner.featureName(), service.containerId());
                    service.close();
                } catch (Exception e) {
                    // We don't want to fail the shutdown hook if a service fails to close
                    logFailedToClose(e, owner.featureName(), owner.launchMode(), service.containerId());
                }
            }
        }
    }

    public Set<RunningService> getAllRunningServices(String launchMode) {
        return servicesIndexedByLaunchMode.getOrDefault(launchMode, Collections.emptySet());
    }

    public RunningService getRunningServices(ComparableDevServicesConfig identifyingConfig) {
        return servicesIndexedByConfig.get(identifyingConfig);
    }

    public void addRunningService(ComparableDevServicesConfig key, RunningService service) {
        servicesIndexedByConfig.put(key, service);
        servicesIndexedByLaunchMode.computeIfAbsent(key.owner().launchMode(), k -> new HashSet<>()).add(service);
    }

}
