package io.quarkus.annotation.processor.documentation.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ConfigSection extends AbstractConfigItem implements ConfigItemCollection {

    private boolean generated;
    private final List<AbstractConfigItem> items = new ArrayList<>();
    private final int level;

    public ConfigSection(String sourceType, String sourceElementName, SourceElementType sourceElementType, SectionPath path,
            String type, int level, boolean generated, Deprecation deprecation) {
        super(sourceType, sourceElementName, sourceElementType, path, type, deprecation);
        this.generated = generated;
        this.level = level;
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

    public SectionPath getPath() {
        return (SectionPath) super.getPath();
    }

    public boolean isSection() {
        return true;
    }

    public boolean isGenerated() {
        return generated;
    }

    public int getLevel() {
        return level;
    }

    /**
     * This is used when we merge ConfigSection at the ConfigRoot level.
     * It can happen when for instance a path is both used at a given level and in an unnamed map.
     * For instance in: HibernateOrmConfig.
     */
    public void appendState(boolean generated, Deprecation deprecation) {
        // we generate the section if at least one of the sections should be generated
        // (the output will contain all the items of the section)
        this.generated = this.generated || generated;
        // we unmark the section as deprecated if one of the merged section is not deprecated
        // as we will have to generate the section
        this.deprecation = this.deprecation != null && deprecation != null ? this.deprecation : null;
    }

    /**
     * This is used to merge ConfigRoot when generating the AsciiDoc output.
     */
    public void merge(ConfigSection other, Map<String, ConfigSection> existingConfigSections) {
        this.generated = this.generated || other.generated;

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

    @Override
    public boolean hasDurationType() {
        for (AbstractConfigItem item : items) {
            if (item.hasDurationType() && !item.isDeprecated()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasMemorySizeType() {
        for (AbstractConfigItem item : items) {
            if (item.hasMemorySizeType() && !item.isDeprecated()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void walk(ConfigItemVisitor visitor) {
        visitor.visit(this);
        for (AbstractConfigItem item : items) {
            item.walk(visitor);
        }
    }

    public record SectionPath(String property) implements Path {

        @Override
        public String toString() {
            return property();
        }
    }
}
