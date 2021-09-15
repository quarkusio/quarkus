package io.quarkus.bootstrap.model;

import java.util.Collection;

public interface ExtensionCapabilities {

    String getExtension();

    Collection<String> getProvidesCapabilities();
}
