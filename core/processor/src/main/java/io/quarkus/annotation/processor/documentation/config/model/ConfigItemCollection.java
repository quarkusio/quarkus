package io.quarkus.annotation.processor.documentation.config.model;

import java.util.List;
import java.util.Map;

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

    @JsonIgnore
    default List<AbstractConfigItem> getNonDeprecatedProperties() {
        return getItems().stream()
                .filter(i -> i instanceof ConfigProperty && !i.isDeprecated())
                .toList();
    }

    @JsonIgnore
    default List<AbstractConfigItem> getNonDeprecatedSections() {
        return getItems().stream()
                .filter(i -> i instanceof ConfigSection && !i.isDeprecated())
                .toList();
    }

    void addItem(AbstractConfigItem item);

    Map<String, String> getAttributes();

    boolean hasDurationType();

    boolean hasMemorySizeType();
}
