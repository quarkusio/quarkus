package io.quarkus.annotation.processor.documentation.config.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.annotation.processor.documentation.config.model.Extension;

public sealed abstract class DiscoveryRootElement permits DiscoveryConfigRoot, DiscoveryConfigGroup {

    private final Extension extension;
    private final String binaryName;
    private final String qualifiedName;
    private String unresolvedSuperclass;
    private final List<String> unresolvedInterfaces = new ArrayList<>();
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

    public void setUnresolvedSuperclass(String unresolvedSuperclass) {
        this.unresolvedSuperclass = unresolvedSuperclass;
    }

    public String getUnresolvedSuperclass() {
        return unresolvedSuperclass;
    }

    public void addUnresolvedInterfaces(String unresolvedInterface) {
        unresolvedInterfaces.add(unresolvedInterface);
    }

    public List<String> getUnresolvedInterfaces() {
        return Collections.unmodifiableList(unresolvedInterfaces);
    }

    public void addProperty(DiscoveryConfigProperty discoveryConfigProperty) {
        properties.put(discoveryConfigProperty.getSourceName(), discoveryConfigProperty);
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
        if (this.unresolvedSuperclass != null) {
            sb.append("\n");
            sb.append(prefix + "unresolvedSuperclass = " + this.unresolvedSuperclass);
        }
        if (!this.unresolvedInterfaces.isEmpty()) {
            sb.append("\n");
            sb.append(prefix + "unresolvedInterfaces = " + this.unresolvedInterfaces);
        }

        if (!properties.isEmpty()) {
            sb.append("\n\n" + prefix + "--- Properties ---\n\n");
            for (DiscoveryConfigProperty property : properties.values()) {
                sb.append(property.toString(prefix) + prefix + "--\n");
            }
        }

        return sb.toString();
    }
}
