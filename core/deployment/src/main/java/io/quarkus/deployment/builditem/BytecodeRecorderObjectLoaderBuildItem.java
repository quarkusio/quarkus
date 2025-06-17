package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.recording.ObjectLoader;

/**
 * This class will return the {@link ObjectLoader} of the BuildItem.
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
