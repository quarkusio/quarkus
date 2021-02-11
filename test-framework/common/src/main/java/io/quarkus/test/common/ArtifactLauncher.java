package io.quarkus.test.common;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public interface ArtifactLauncher extends Closeable {

    void start() throws IOException;

    void addSystemProperties(Map<String, String> systemProps);

    boolean isDefaultSsl();
}
