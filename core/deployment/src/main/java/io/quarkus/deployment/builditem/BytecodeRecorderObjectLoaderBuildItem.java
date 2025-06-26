package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.recording.ObjectLoader;

/**
 * Used to record the bytecode of Object Loaders loaded in the application context
 *
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
