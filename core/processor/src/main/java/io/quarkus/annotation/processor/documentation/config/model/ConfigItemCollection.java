package io.quarkus.annotation.processor.documentation.config.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ConfigItemCollection {

    List<AbstractConfigItem> getItems();

    @JsonIgnore
    default List<AbstractConfigItem> getNonDeprecatedItems() {
        return getItems().stream()
                .filter(i -> (i instanceof ConfigSection)
                        ? !i.isDeprecated() && ((ConfigSection) i).getNonDeprecatedItems().size() > 0
                        : !i.isDeprecated())
                .toList();
    }

    void addItem(AbstractConfigItem item);

    boolean hasDurationType();

    boolean hasMemorySizeType();
}
