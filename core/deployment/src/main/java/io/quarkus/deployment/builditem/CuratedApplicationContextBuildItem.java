package io.quarkus.deployment.builditem;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that provides a per-{@link io.quarkus.bootstrap.app.CuratedApplication} context map.
 * <p>
 * Unlike {@link LiveReloadBuildItem}, whose context is scoped to a single
 * {@code AugmentActionImpl} (and thus reset when a new one is created),
 * this context survives across {@code AugmentActionImpl} instances for
 * the lifetime of the {@code CuratedApplication}.
 * <p>
 * Use this for state that must persist across continuous testing restarts
 * but be isolated between different {@code CuratedApplication} instances.
 */
public final class CuratedApplicationContextBuildItem extends SimpleBuildItem {

    private final Map<Class<?>, Object> curatedApplicationContext;

    public CuratedApplicationContextBuildItem(Map<Class<?>, Object> curatedApplicationContext) {
        this.curatedApplicationContext = curatedApplicationContext;
    }

    @SuppressWarnings("unchecked")
    public <T> T getContextObject(Class<T> type) {
        return (T) curatedApplicationContext.get(type);
    }

    public <T> void setContextObject(Class<T> type, T val) {
        curatedApplicationContext.put(type, val);
    }
}
