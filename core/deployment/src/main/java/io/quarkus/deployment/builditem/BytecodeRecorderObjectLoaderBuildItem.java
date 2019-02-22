package io.quarkus.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;

import io.quarkus.deployment.recording.ObjectLoader;

/**
 */
public final class BytecodeRecorderObjectLoaderBuildItem extends MultiBuildItem {
    private final ObjectLoader objectLoader;

    public BytecodeRecorderObjectLoaderBuildItem(final ObjectLoader objectLoader) {
        this.objectLoader = objectLoader;
    }

    public ObjectLoader getObjectLoader() {
        return objectLoader;
    }
}
