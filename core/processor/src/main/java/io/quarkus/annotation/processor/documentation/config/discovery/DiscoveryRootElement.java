package io.quarkus.annotation.processor.documentation.config.discovery;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.annotation.processor.documentation.config.model.Extension;

public sealed abstract class DiscoveryRootElement permits DiscoveryConfigRoot, DiscoveryConfigGroup {

    private final Extension extension;
    private final String binaryName;
    private final String qualifiedName;
    private final Map<String, DiscoveryConfigProperty> properties = new LinkedHashMap<>();

    // TODO #42114 remove once fixed
    // this is an approximation, we can't fully detect that in the case of config groups
    @Deprecated(forRemoval = true)
    private final boolean configMapping;

    DiscoveryRootElement(Extension extension, String binaryName, String qualifiedName, boolean configMapping) {
        this.extension = extension;
        this.binaryName = binaryName;
        this.qualifiedName = qualifiedName;
        this.configMapping = configMapping;
    }

    public Extension getExtension() {
        return extension;
    }

    public String getBinaryName() {
        return binaryName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void addProperty(DiscoveryConfigProperty discoveryConfigProperty) {
        properties.put(discoveryConfigProperty.getSourceElementName(), discoveryConfigProperty);
    }

    public Map<String, DiscoveryConfigProperty> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Deprecated(forRemoval = true)
    public boolean isConfigMapping() {
        return configMapping;
    }

    public String toString() {
        return toString("");
    }

    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix + "binaryName = " + this.binaryName);

        if (!properties.isEmpty()) {
            sb.append("\n\n" + prefix + "--- Properties ---\n\n");
            for (DiscoveryConfigProperty property : properties.values()) {
                sb.append(property.toString(prefix) + prefix + "--\n");
            }
        }

        return sb.toString();
    }
}
