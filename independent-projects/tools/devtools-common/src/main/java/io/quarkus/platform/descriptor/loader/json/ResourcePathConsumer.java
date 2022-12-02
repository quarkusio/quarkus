package io.quarkus.platform.descriptor.loader.json;

import java.io.IOException;
import java.nio.file.Path;

public interface ResourcePathConsumer<T> {

    T consume(Path is) throws IOException;
}
