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

    DiscoveryRootElement(Extension extension, String binaryName, String qualifiedName) {
        this.extension = extension;
        this.binaryName = binaryName;
        this.qualifiedName = qualifiedName;
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
