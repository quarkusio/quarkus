package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.recording.ObjectLoader;

public final class BytecodeRecorderObjectLoaderBuildItem extends MultiBuildItem {
    private final ObjectLoader objectLoader;

    public BytecodeRecorderObjectLoaderBuildItem(final ObjectLoader objectLoader) {
        this.objectLoader = objectLoader;
    }

    /**
     * Returns the {@link ObjectLoader} of the BuildItem.
     *
     * @return The {@link ObjectLoader} of the BuildItem.
     */
    public ObjectLoader getObjectLoader() {
        return objectLoader;
    }
}
