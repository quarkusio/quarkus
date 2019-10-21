package io.quarkus.platform.descriptor;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceInputStreamConsumer<T> {

    T handle(InputStream is) throws IOException;
}
