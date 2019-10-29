package io.quarkus.platform.descriptor.loader.json.impl;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceLoader {

    InputStream getResourceAsStream(String name) throws IOException;
}
