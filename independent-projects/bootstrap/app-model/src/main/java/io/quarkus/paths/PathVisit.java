package io.quarkus.paths;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

public interface PathVisit {

    Path getRoot();

    Path getPath();

    default URL getUrl() {
        try {
            return getPath().toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to translate " + getPath().toUri() + " to " + URL.class.getName(), e);
        }
    }

    String getRelativePath(String separator);

    void stopWalking();
}
