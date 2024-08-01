package io.quarkus.annotation.processor.documentation.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * At this stage, a config root is actually a prefix: we merged all the config roots with the same prefix.
 * <p>
 * Thus the phase for instance is not stored at this level but at the item level.
 */
public class ConfigRoot implements ConfigItemCollection {

    private final Extension extension;
    private final String prefix;

    private String overriddenDocFileName;
    private final List<AbstractConfigItem> items = new ArrayList<>();
    private final Set<String> qualifiedNames = new HashSet<>();

    public ConfigRoot(Extension extension, String prefix) {
        this.extension = extension;
        this.prefix = prefix;
    }

    public Extension getExtension() {
        return extension;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setOverriddenDocFileName(String overriddenDocFileName) {
        if (this.overriddenDocFileName != null) {
            return;
        }
        this.overriddenDocFileName = overriddenDocFileName;
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

    public void merge(ConfigRoot other) {
        this.qualifiedNames.addAll(other.getQualifiedNames());

        for (AbstractConfigItem otherItem : other.getItems()) {
            if (otherItem instanceof ConfigSection configSection) {
                Optional<ConfigSection> similarConfigSection = findSimilarSection(configSection);
                if (similarConfigSection.isEmpty()) {
                    this.items.add(configSection);
                } else {
                    similarConfigSection.get().merge(configSection);
                }
            } else if (otherItem instanceof ConfigProperty configProperty) {
                this.items.add(configProperty);
            } else {
                throw new IllegalStateException("Unknown item type: " + otherItem.getClass());
            }
        }

        Collections.sort(this.items);
    }

    private Optional<ConfigSection> findSimilarSection(ConfigSection configSection) {
        // this is a bit naive as a section could be nested differently but with a similar path
        // it should be sufficient for now
        // also, it's not exactly optimal, maybe we should have a map (but we need to be careful about the order), we'll see
        return this.getItems().stream()
                .filter(i -> i.isSection())
                .filter(i -> i.getPath().equals(configSection.getPath()))
                .map(i -> (ConfigSection) i)
                .findFirst();
    }

    public boolean hasDurationType() {
        for (AbstractConfigItem item : items) {
            if (item.hasDurationType() && !item.deprecated) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMemorySizeType() {
        for (AbstractConfigItem item : items) {
            if (item.hasMemorySizeType() && !item.deprecated) {
                return true;
            }
        }
        return false;
    }
}
