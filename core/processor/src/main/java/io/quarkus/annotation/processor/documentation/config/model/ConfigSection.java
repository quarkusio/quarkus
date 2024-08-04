package io.quarkus.annotation.processor.documentation.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ConfigSection extends AbstractConfigItem implements ConfigItemCollection {

    private boolean generated;
    private final List<AbstractConfigItem> items = new ArrayList<>();

    public ConfigSection(String sourceClass, String sourceName, String path, String type, boolean generated,
            boolean deprecated) {
        super(sourceClass, sourceName, path, type, deprecated);
        this.generated = generated;
    }

    @Override
    public void addItem(AbstractConfigItem item) {
        this.items.add(item);
    }

    @Override
    public List<AbstractConfigItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public int compareTo(AbstractConfigItem o) {
        if (o instanceof ConfigProperty) {
            return 1;
        }

        return 0;
    }

    public boolean isSection() {
        return true;
    }

    public boolean isGenerated() {
        return generated;
    }

    public void merge(ConfigSection other) {
        this.generated = this.generated || other.generated;

        for (AbstractConfigItem otherItem : other.getItems()) {
            if (otherItem instanceof ConfigSection configSection) {
                Optional<ConfigSection> similarConfigSection = findSimilarSection(configSection);
                if (similarConfigSection.isEmpty()) {
                    this.items.add(configSection);
                } else {
                    similarConfigSection.get().merge(configSection);
                    Collections.sort(similarConfigSection.get().items);
                }
            } else if (otherItem instanceof ConfigProperty configProperty) {
                this.items.add(configProperty);
            } else {
                throw new IllegalStateException("Unknown item type: " + otherItem.getClass());
            }
        }
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

    @Override
    public boolean hasDurationType() {
        for (AbstractConfigItem item : items) {
            if (item.hasDurationType() && !item.deprecated) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasMemorySizeType() {
        for (AbstractConfigItem item : items) {
            if (item.hasMemorySizeType() && !item.deprecated) {
                return true;
            }
        }
        return false;
    }
}
