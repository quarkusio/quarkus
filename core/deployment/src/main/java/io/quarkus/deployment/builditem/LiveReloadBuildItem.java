package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that can be used to query the live reload state.
 *
 *
 * It can also be used to store context information that is persistent between hot reloads.
 */
public final class LiveReloadBuildItem extends SimpleBuildItem {

    private final boolean liveReload;
    private final Set<String> changedResources;
    private final Map<Class<?>, Object> reloadContext;

    /**
     * This constructor should only be used if live reload is not possible
     */
    public LiveReloadBuildItem() {
        liveReload = false;
        changedResources = Collections.emptySet();
        this.reloadContext = new ConcurrentHashMap<>();
    }

    public LiveReloadBuildItem(boolean liveReload, Set<String> changedResources, Map<Class<?>, Object> reloadContext) {
        this.liveReload = liveReload;
        this.changedResources = changedResources;
        this.reloadContext = reloadContext;
    }

    /**
     * If this is a reload of an app in the same JVM then this will return true. If it is the first
     * time this app has started, or the app is not running in developer mode it will return false.
     *
     * @return <code>true</code> if this is a live reload
     */
    public boolean isLiveReload() {
        return liveReload;
    }

    /**
     * If this is a live reload this set contains the config resources that have changed
     *
     */
    public Set<String> getChangedResources() {
        return changedResources;
    }

    /**
     * Gets an object from live reload context that is persistent across restarts
     *
     * @return
     */
    public <T> T getContextObject(Class<T> type) {
        return (T) reloadContext.get(type);
    }

    /**
     * Sets an object into the live reload context that is persistent across restarts
     *
     */
    public <T> void setContextObject(Class<T> type, T val) {
        reloadContext.put(type, val);
    }
}
