package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.recording.ObjectLoader;

/**
 * @deprecated This class is intended only for internal use.
 */
@Deprecated(forRemoval = true)
public final class BytecodeRecorderObjectLoaderBuildItem extends MultiBuildItem {
    private final ObjectLoader objectLoader;

    public BytecodeRecorderObjectLoaderBuildItem(final ObjectLoader objectLoader) {
        this.objectLoader = objectLoader;
    }

    public ObjectLoader getObjectLoader() {
        return objectLoader;
    }
}
