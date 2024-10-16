package io.quarkus.observability.devresource;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

/**
 * A registry of dev resources.
 */
@SuppressWarnings("rawtypes")
public class DevResources {
    private static final Logger log = Logger.getLogger(DevResources.class);

    private static List<DevResourceLifecycleManager> resources;
    private static Map<String, String> map;

    /**
     * @return list of found dev resources.
     */
    public static synchronized List<DevResourceLifecycleManager> resources() {
        if (resources == null) {
            log.debug("Activating dev resources");

            resources = ServiceLoader
                    .load(DevResourceLifecycleManager.class, Thread.currentThread().getContextClassLoader())
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .sorted(Comparator.comparing(DevResourceLifecycleManager::order))
                    .collect(Collectors.toList());

            log.debugf("Found dev resources: %s", resources);
        }
        return resources;
    }

    /**
     * Ensures all dev resources are started and returns a map of config properties.
     *
     * @return a map of config properties to be returned by {@link DevResourcesConfigSource}
     */
    static synchronized Map<String, String> ensureStarted() {
        if (map == null) {
            try {
                for (var res : resources()) {
                    res.initDev();
                }
            } catch (Exception e) {
                log.error("Exception initializing dev resource manager", e);
                throw e;
            }
            try {
                var map = new HashMap<String, String>();
                for (var res : resources()) {
                    var resMap = res.start();
                    log.infof("Dev resource [%s] contributed config: %s", res.getClass().getSimpleName(), resMap);
                    map.putAll(resMap);
                }
                DevResources.map = Collections.unmodifiableMap(map);
            } catch (Exception e) {
                log.error("Exception starting dev resource", e);
                throw e;
            }
        }
        return map;
    }

    /**
     * Stops all dev resources.
     */
    public static synchronized void stop() {
        if (map != null) {
            for (var i = resources().listIterator(resources().size()); i.hasPrevious();) {
                try {
                    i.previous().stop();
                } catch (Exception e) {
                    log.warn("Exception stopping dev resource", e);
                }
            }
            map = null;
        }
    }
}
