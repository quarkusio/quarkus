package io.quarkus.devservices.crossclassloader.runtime;

import static java.util.UUID.randomUUID;

import java.util.Collection;
import java.util.Collections;
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

    /**
     * Closes the services owned by the given application instance, as the shutdown close task.
     * <p>
     * Services configured for Testcontainers-level reuse are removed from the registry but left running,
     * so that a later, separate JVM run can pick them up. They are still stopped by
     * {@link #closeAllRunningServices(DevServiceOwner)} when their configuration no longer matches the
     * application being started, such as on an incompatible profile change.
     */
    public void closeOwnRunningServices(UUID uuid, String launchMode) {
        Set<RunningService> services = servicesIndexedByLaunchMode.get(launchMode);
        Iterator<Map.Entry<ComparableDevServicesConfig, RunningService>> it = servicesIndexedByConfig.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ComparableDevServicesConfig, RunningService> next = it.next();
            DevServiceOwner owner = next.getKey().owner();
            UUID serviceAppUuid = next.getKey().applicationInstanceId();
            if (owner.launchMode().equals(launchMode) && Objects.equals(serviceAppUuid, uuid)) {
                it.remove();
                RunningService service = next.getValue();
                if (services != null) {
                    services.remove(service);
                }
                if (service.isReusable()) {
                    log.debugf("Not closing reusable dev service for %s in launch mode %s: %s",
                            owner.featureName(), launchMode, service.containerId());
                } else {
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

    /**
     * Closes every service belonging to the given owner, regardless of reuse configuration.
     * <p>
     * This runs when a service's configuration no longer matches the application being started, such as
     * on an incompatible profile change, where the running container cannot satisfy the new configuration
     * and has to be replaced. Unlike the shutdown path in {@link #closeOwnRunningServices(UUID, String)},
     * reuse does not spare a service here.
     */
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
                    logClosing(owner.featureName(), owner.launchMode(), service.containerId());
                    service.close();
                } catch (Exception e) {
                    logFailedToClose(e, owner.featureName(), owner.launchMode(), service.containerId());
                }
            }
        }
    }

    public void closeOwnRunningServices(UUID uuid, DevServiceOwner owner) {
        Set<RunningService> launchModeServices = servicesIndexedByLaunchMode.get(owner.launchMode());
        var iterator = servicesIndexedByConfig.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ComparableDevServicesConfig, RunningService> entry = iterator.next();
            DevServiceOwner entryOwner = entry.getKey().owner();
            UUID serviceAppUuid = entry.getKey().applicationInstanceId();
            if (Objects.equals(entryOwner, owner) && Objects.equals(serviceAppUuid, uuid)) {
                iterator.remove();
                RunningService service = entry.getValue();
                if (launchModeServices != null) {
                    launchModeServices.remove(service);
                }
                try {
                    logClosing(owner.featureName(), owner.launchMode(), service.containerId());
                    service.close();
                } catch (Exception e) {
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

    /**
     * Hands a value of the running application to one of its dev services, such as a value that is only known at
     * runtime. Called from the application on every start, so a service reused across dev mode restarts gets the value
     * of every start. Nothing happens if the dev service is not running, for instance when dev services are disabled.
     *
     * @param applicationInstanceId the id of the application instance that owns the service, as a {@code String}
     *        since a service of the application cannot capture a {@link UUID}. Each curated application has its own id,
     *        so it also tells dev mode apart from its continuous testing
     * @param featureName the name of the feature of the dev service, such as {@code rest-client}
     * @param serviceName the name of the dev service
     * @param key the key of the value, as agreed between the dev service and the application
     * @param value the value
     */
    public void publish(String applicationInstanceId, String featureName, String serviceName, String key, String value) {
        for (Map.Entry<ComparableDevServicesConfig, RunningService> entry : servicesIndexedByConfig.entrySet()) {
            DevServiceOwner owner = entry.getKey().owner();
            if (entry.getKey().applicationInstanceId().toString().equals(applicationInstanceId)
                    && owner.featureName().equals(featureName)
                    && Objects.equals(owner.configName(), serviceName)) {
                entry.getValue().valueAvailable(key, value);
                log.debugf("Published %s=%s to dev service %s of %s in launch mode %s", key, value, serviceName,
                        featureName, owner.launchMode());
            }
        }
    }

    public void addRunningService(ComparableDevServicesConfig key, RunningService service) {
        servicesIndexedByConfig.put(key, service);
        // Be aware of thread safety; if there are multiple services this method will be accessed by multiple threads; we use the ConcurrentHashMap's thread-safe set instead of HashSet
        servicesIndexedByLaunchMode.computeIfAbsent(key.owner().launchMode(), k -> ConcurrentHashMap.newKeySet()).add(service);
    }

}
