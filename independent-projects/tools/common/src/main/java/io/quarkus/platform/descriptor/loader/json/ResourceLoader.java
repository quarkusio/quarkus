package io.quarkus.platform.descriptor.loader.json;

import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;
import java.io.IOException;

public interface ResourceLoader {

    <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException;
}
