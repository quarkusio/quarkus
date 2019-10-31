package io.quarkus.platform.descriptor;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceInputStreamConsumer<T> {

    T consume(InputStream is) throws IOException;
}
