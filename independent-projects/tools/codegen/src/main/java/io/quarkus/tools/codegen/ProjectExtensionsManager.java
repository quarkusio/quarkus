package io.quarkus.tools.codegen;

import io.quarkus.dependencies.Extension;
import java.io.Closeable;

public interface ProjectExtensionsManager extends Closeable {

    void addExtension(Extension extension);

    boolean hasExtension(Extension extension);

    void removeExtension(Extension extension);
}
