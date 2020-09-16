package io.quarkus.platform.descriptor;

import java.io.IOException;
import java.nio.file.Path;

public interface ResourcePathConsumer<T> {

    T consume(Path is) throws IOException;
}
