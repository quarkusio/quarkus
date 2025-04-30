package io.quarkus.annotation.processor.documentation.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import io.quarkus.annotation.processor.documentation.config.util.Markers;

/**
 * At this stage, a config root is actually a prefix: we merged all the config roots with the same prefix.
 * <p>
 * Thus the phase for instance is not stored at this level but at the item level.
 */
public class ConfigRoot implements ConfigItemCollection {

    private final Extension extension;
    private final String prefix;
    // used by the doc generation to classify config roots
    private final String topLevelPrefix;

    private final String overriddenDocFileName;
    private final List<AbstractConfigItem> items = new ArrayList<>();
    private final Set<String> qualifiedNames = new HashSet<>();

    public ConfigRoot(Extension extension, String prefix, String overriddenDocPrefix, String overriddenDocFileName) {
        this.extension = extension;
        this.prefix = prefix;
        this.overriddenDocFileName = overriddenDocFileName;
        this.topLevelPrefix = overriddenDocPrefix != null ? buildTopLevelPrefix(overriddenDocPrefix)
                : buildTopLevelPrefix(prefix);
    }

    public Extension getExtension() {
        return extension;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getOverriddenDocFileName() {
        return overriddenDocFileName;
    }

    public void addQualifiedName(String qualifiedName) {
        qualifiedNames.add(qualifiedName);
    }

    public Set<String> getQualifiedNames() {
        return Collections.unmodifiableSet(qualifiedNames);
    }

    @Override
    public void addItem(AbstractConfigItem item) {
        this.items.add(item);
    }

    @Override
    public List<AbstractConfigItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public String getTopLevelPrefix() {
        return topLevelPrefix;
    }

    public void merge(ConfigRoot other) {
        this.qualifiedNames.addAll(other.getQualifiedNames());

        Map<String, ConfigSection> existingConfigSections = new HashMap<>();
        collectConfigSections(existingConfigSections, this);

        for (AbstractConfigItem otherItem : other.getItems()) {
            if (otherItem instanceof ConfigSection otherConfigSection) {
                ConfigSection similarConfigSection = existingConfigSections.get(otherConfigSection.getPath().property());

                if (similarConfigSection == null) {
                    this.items.add(otherConfigSection);
                } else {
                    similarConfigSection.merge(otherConfigSection, existingConfigSections);
                }
            } else if (otherItem instanceof ConfigProperty configProperty) {
                this.items.add(configProperty);
            } else {
                throw new IllegalStateException("Unknown item type: " + otherItem.getClass());
            }
        }

        Collections.sort(this.items);
    }

    private void collectConfigSections(Map<String, ConfigSection> configSections, ConfigItemCollection configItemCollection) {
        for (AbstractConfigItem item : configItemCollection.getItems()) {
            if (item instanceof ConfigSection configSection) {
                configSections.put(item.getPath().property(), configSection);

                collectConfigSections(configSections, configSection);
            }
        }
    }

    public boolean hasDurationType() {
        for (AbstractConfigItem item : items) {
            if (item.hasDurationType() && !item.isDeprecated()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMemorySizeType() {
        for (AbstractConfigItem item : items) {
            if (item.hasMemorySizeType() && !item.isDeprecated()) {
                return true;
            }
        }
        return false;
    }

    private static String buildTopLevelPrefix(String prefix) {
        String[] prefixSegments = prefix.split(Pattern.quote(Markers.DOT));

        if (prefixSegments.length == 1) {
            return prefixSegments[0];
        }

        return prefixSegments[0] + Markers.DOT + prefixSegments[1];
    }

    public void walk(ConfigItemVisitor visitor) {
        for (AbstractConfigItem item : items) {
            item.walk(visitor);
        }
    }
}
