package io.quarkus.annotation.processor.documentation.config.model;

import java.util.List;

public interface ConfigItemCollection {

    List<AbstractConfigItem> getItems();

    void addItem(AbstractConfigItem item);

    boolean hasDurationType();

    boolean hasMemorySizeType();
}
