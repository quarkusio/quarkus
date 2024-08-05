package io.quarkus.annotation.processor.documentation.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    /**
     * This is used when we merge ConfigSection at the ConfigRoot level.
     * It can happen when for instance a path is both used at a given level and in an unnamed map.
     * For instance in: HibernateOrmConfig.
     */
    public void appendState(boolean generated, boolean deprecated) {
        // we generate the section if at least one of the sections should be generated
        // (the output will contain all the items of the section)
        this.generated = this.generated || generated;
        // we unmark the section as deprecated if one of the merged section is not deprecated
        // as we will have to generate the section
        this.deprecated = this.deprecated && deprecated;
    }

    /**
     * This is used to merge ConfigRoot when generating the AsciiDoc output.
     */
    public void merge(ConfigSection other, Map<String, ConfigSection> existingConfigSections) {
        this.generated = this.generated || other.generated;

        for (AbstractConfigItem otherItem : other.getItems()) {
            if (otherItem instanceof ConfigSection otherConfigSection) {
                ConfigSection similarConfigSection = existingConfigSections.get(otherConfigSection.getPath());
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
