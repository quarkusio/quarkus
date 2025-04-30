package io.quarkus.annotation.processor.documentation.config.scanner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigGroup;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigRoot;
import io.quarkus.annotation.processor.documentation.config.discovery.EnumDefinition;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;

public class ConfigCollector {

    /**
     * Key is qualified name of the class + "." + element name (for instance field or method name)
     */
    private Map<String, JavadocElement> javadocElements = new HashMap<>();

    /**
     * Key is the qualified name of the class.
     */
    private Map<String, DiscoveryConfigRoot> configRoots = new HashMap<>();

    /**
     * Key is the qualified name of the class.
     */
    private Map<String, DiscoveryConfigGroup> resolvedConfigGroups = new HashMap<>();

    /**
     * Key is the qualified name of the class.
     */
    private Map<String, EnumDefinition> resolvedEnums = new HashMap<>();

    public void addJavadocElement(String key, JavadocElement element) {
        javadocElements.put(key, element);
    }

    public Map<String, JavadocElement> getJavadocElements() {
        return Collections.unmodifiableMap(javadocElements);
    }

    public void addConfigRoot(DiscoveryConfigRoot configRoot) {
        configRoots.put(configRoot.getQualifiedName(), configRoot);
    }

    public Collection<DiscoveryConfigRoot> getConfigRoots() {
        return Collections.unmodifiableCollection(configRoots.values());
    }

    public void addResolvedConfigGroup(DiscoveryConfigGroup configGroup) {
        resolvedConfigGroups.put(configGroup.getQualifiedName(), configGroup);
    }

    public Collection<DiscoveryConfigGroup> getResolvedConfigGroups() {
        return Collections.unmodifiableCollection(resolvedConfigGroups.values());
    }

    public DiscoveryConfigGroup getResolvedConfigGroup(String configGroupClassName) {
        return resolvedConfigGroups.get(configGroupClassName);
    }

    public boolean isConfigGroup(String className) {
        return isResolvedConfigGroup(className);
    }

    public boolean isResolvedConfigGroup(String className) {
        return resolvedConfigGroups.containsKey(className);
    }

    public void addResolvedEnum(EnumDefinition enumDefinition) {
        resolvedEnums.put(enumDefinition.qualifiedName(), enumDefinition);
    }

    public boolean isEnum(String className) {
        return isResolvedEnum(className);
    }

    public boolean isResolvedEnum(String className) {
        return resolvedEnums.containsKey(className);
    }

    public EnumDefinition getResolvedEnum(String name) {
        EnumDefinition enumDefinition = resolvedEnums.get(name);

        if (enumDefinition == null) {
            throw new IllegalStateException("Could not find registered EnumDefinition for " + name);
        }

        return enumDefinition;
    }

    public Map<String, EnumDefinition> getResolvedEnums() {
        return resolvedEnums;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("=======================================================\n");
        sb.append("= Config roots\n");
        sb.append("=======================================================\n\n");

        for (DiscoveryConfigRoot configRoot : configRoots.values()) {
            sb.append("- " + configRoot.getQualifiedName() + "\n");
            sb.append(configRoot.toString("  "));
            sb.append("\n\n===\n\n");
        }
        if (configRoots.isEmpty()) {
            sb.append(" No config roots were detected\n\n");
        }

        sb.append("=======================================================\n");
        sb.append("= Config groups\n");
        sb.append("=======================================================\n\n");

        for (DiscoveryConfigGroup configGroup : resolvedConfigGroups.values()) {
            sb.append("- " + configGroup.getQualifiedName() + "\n");
            sb.append(configGroup.toString("  "));
            sb.append("\n\n===\n\n");
        }

        if (resolvedConfigGroups.isEmpty()) {
            sb.append(" No config groups were detected\n\n");
        }

        return sb.toString();
    }
}
