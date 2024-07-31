package io.quarkus.annotation.processor.documentation.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigSection extends AbstractConfigItem implements ConfigItemCollection {

    private final List<AbstractConfigItem> items = new ArrayList<>();

    public ConfigSection(String sourceClass, String sourceName, String path, String type) {
        super(sourceClass, sourceName, path, type);
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
}
