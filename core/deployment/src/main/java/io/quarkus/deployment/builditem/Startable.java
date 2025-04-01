package io.quarkus.deployment.builditem;

import java.io.Closeable;

public interface Startable extends Closeable {
    void start();
}
