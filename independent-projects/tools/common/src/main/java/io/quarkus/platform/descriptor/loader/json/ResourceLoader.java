package io.quarkus.platform.descriptor.loader.json;

import java.io.IOException;
import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;

public interface ResourceLoader {

    <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException;
}
