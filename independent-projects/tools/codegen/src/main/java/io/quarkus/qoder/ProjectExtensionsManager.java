package io.quarkus.qoder;

import io.quarkus.dependencies.Extension;

public interface ProjectExtensionsManager {

    void addExtension(Extension extension);

    boolean hasExtension(Extension extension);

    void removeExtension(Extension extension);
}
