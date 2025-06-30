package io.quarkus.deployment.builditem;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that carries all the "fake" run time config objects for use by recorders.
 */
@Deprecated(forRemoval = true, since = "3.25")
public final class RunTimeConfigurationProxyBuildItem extends SimpleBuildItem {
    private final Map<Class<?>, Object> objects;

    public RunTimeConfigurationProxyBuildItem(final Map<Class<?>, Object> objects) {
        this.objects = objects;
    }

    public Object getProxyObjectFor(Class<?> clazz) {
        return objects.get(clazz);
    }
}
