package io.quarkus.bootstrap.workspace;

import java.io.File;

public interface ProcessedSources {

    File getSourceDir();

    File getDestinationDir();

    default <T> T getValue(Object key, Class<T> type) {
        return null;
    }
}
