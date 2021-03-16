package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.bootstrap.app.ClassChangeInformation;
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
    private final ClassChangeInformation changeInformation;

    /**
     * This constructor should only be used if live reload is not possible
     */
    public LiveReloadBuildItem() {
        liveReload = false;
        changedResources = Collections.emptySet();
        this.reloadContext = new ConcurrentHashMap<>();
        this.changeInformation = null;
    }

    public LiveReloadBuildItem(boolean liveReload, Set<String> changedResources, Map<Class<?>, Object> reloadContext,
            ClassChangeInformation changeInformation) {
        this.liveReload = liveReload;
        this.changedResources = changedResources;
        this.reloadContext = reloadContext;
        this.changeInformation = changeInformation;
    }

    /**
     * If this is a reload of an app in the same JVM then this will return true. If it is the first
     * time this app has started, or the app is not running in developer mode it will return false.
     *
     * Note that unsuccessful attempts to start are not counted, if if the app initially failed to start
     * the next attempt this will still return false.
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

    /**
     * Returns the change information from the last successful restart.
     *
     * Will be null if Quarkus has not previously successfully started, or if the
     * previous attempt to start was a failure.
     *
     */
    public ClassChangeInformation getChangeInformation() {
        return changeInformation;
    }
}
