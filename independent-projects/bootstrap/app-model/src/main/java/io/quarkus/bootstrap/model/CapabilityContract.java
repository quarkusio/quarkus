package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class CapabilityContract implements ExtensionCapabilities, Serializable {

    private static final long serialVersionUID = -2817736967526011849L;

    private static final Pattern capabilitiesPattern = Pattern.compile("\\s*,\\s*");

    public static CapabilityContract of(String extension, String providesStr, String requiresStr) {

        final Collection<String> provides = parseCapabilities(providesStr);
        for (String name : provides) {
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Extension " + extension
                        + " was configured to provide a capability with an empty name: " + providesStr);
            }
        }
        final Collection<String> requires = parseCapabilities(requiresStr);
        for (String name : requires) {
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Extension " + extension
                        + " was configured to require a capability with an empty name: " + requiresStr);
            }
        }

        return new CapabilityContract(extension, provides, requires);
    }

    private static Collection<String> parseCapabilities(String s) {
        return s == null || s.isBlank() ? List.of() : Arrays.asList(capabilitiesPattern.split(s));
    }

    private final String extension;
    private final Collection<String> providesCapabilities;
    private final Collection<String> requiresCapabilities;

    public CapabilityContract(String extension, Collection<String> providesCapabilities,
            Collection<String> requiresCapabilities) {
        this.extension = Objects.requireNonNull(extension, "extension can't be null");
        this.providesCapabilities = Objects.requireNonNull(providesCapabilities, "providesCapabilities can't be null");
        this.requiresCapabilities = Objects.requireNonNull(requiresCapabilities, "requiresCapabilities can't be null");
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public Collection<String> getProvidesCapabilities() {
        return providesCapabilities;
    }

    @Override
    public Collection<String> getRequiresCapabilities() {
        return requiresCapabilities;
    }
}
