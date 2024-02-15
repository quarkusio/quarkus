package io.quarkus.annotation.processor.documentation.config.scanner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigGroup;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigRoot;
import io.quarkus.annotation.processor.documentation.config.discovery.EnumDefinition;
import io.quarkus.annotation.processor.documentation.config.discovery.UnresolvedEnumDefinition;

public class ConfigCollector {

    @Deprecated(since = "3.14", forRemoval = true)
    private Properties javadocProperties = new Properties();

    /**
     * Key is the qualified name of the class.
     */
    private Map<String, DiscoveryConfigRoot> configRoots = new HashMap<>();

    /**
     * Key is the qualified name of the class.
     */
    private Map<String, DiscoveryConfigGroup> resolvedConfigGroups = new HashMap<>();

    /**
     * Contains the qualified name of the class.
     */
    private Set<String> unresolvedConfigGroups = new HashSet<>();

    /**
     * Key is the qualified name of the class.
     */
    private Map<String, EnumDefinition> resolvedEnums = new HashMap<>();

    /**
     * Contains the qualified name of the class.
     */
    private Map<String, UnresolvedEnumDefinition> unresolvedEnums = new HashMap<>();

    @Deprecated(since = "3.14", forRemoval = true)
    void addJavadocProperty(String key, String docComment) {
        javadocProperties.put(key, docComment);
    }

    @Deprecated(since = "3.14", forRemoval = true)
    public Properties getJavadocProperties() {
        return javadocProperties;
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

    public void addUnresolvedConfigGroup(String configGroupClassName) {
        unresolvedConfigGroups.add(configGroupClassName);
    }

    public Collection<DiscoveryConfigGroup> getResolvedConfigGroups() {
        return Collections.unmodifiableCollection(resolvedConfigGroups.values());
    }

    public DiscoveryConfigGroup getResolvedConfigGroup(String configGroupClassName) {
        return resolvedConfigGroups.get(configGroupClassName);
    }

    public Set<String> getUnresolvedConfigGroups() {
        return Collections.unmodifiableSet(unresolvedConfigGroups);
    }

    public boolean isConfigGroup(String className) {
        return isResolvedConfigGroup(className) || isUnresolvedConfigGroup(className);
    }

    public boolean isResolvedConfigGroup(String className) {
        return resolvedConfigGroups.containsKey(className);
    }

    public boolean isUnresolvedConfigGroup(String className) {
        return unresolvedConfigGroups.contains(className);
    }

    public void addResolvedEnum(EnumDefinition enumDefinition) {
        resolvedEnums.put(enumDefinition.qualifiedName(), enumDefinition);
    }

    public void addUnresolvedEnum(UnresolvedEnumDefinition unresolvedEnumDefinition) {
        unresolvedEnums.put(unresolvedEnumDefinition.qualifiedName(), unresolvedEnumDefinition);
    }

    public boolean isEnum(String className) {
        return isResolvedEnum(className) || isUnresolvedEnum(className);
    }

    public boolean isResolvedEnum(String className) {
        return resolvedEnums.containsKey(className);
    }

    public boolean isUnresolvedEnum(String className) {
        return unresolvedEnums.containsKey(className);
    }

    public UnresolvedEnumDefinition getUnresolvedEnum(String className) {
        return unresolvedEnums.get(className);
    }

    public EnumDefinition getResolvedEnum(String name) {
        return resolvedEnums.get(name);
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
