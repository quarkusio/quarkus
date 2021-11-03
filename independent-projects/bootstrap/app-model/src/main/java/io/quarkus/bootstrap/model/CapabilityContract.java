package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CapabilityContract implements ExtensionCapabilities, Serializable {

    public static CapabilityContract providesCapabilities(String extension, String commaSeparatedList) {
        final List<String> list = Arrays.asList(commaSeparatedList.split("\\s*,\\s*"));
        for (String provided : list) {
            if (provided.isEmpty()) {
                throw new IllegalArgumentException("Extension " + extension
                        + " was configured to provide a capability with an empty name: " + commaSeparatedList);
            }
        }
        return new CapabilityContract(extension, list);
    }

    private final String extension;
    private final List<String> providesCapabilities;

    public CapabilityContract(String extension, List<String> providesCapabilities) {
        this.extension = Objects.requireNonNull(extension, "extension can't be null");
        this.providesCapabilities = Objects.requireNonNull(providesCapabilities, "providesCapabilities can't be null");
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public List<String> getProvidesCapabilities() {
        return providesCapabilities;
    }
}
