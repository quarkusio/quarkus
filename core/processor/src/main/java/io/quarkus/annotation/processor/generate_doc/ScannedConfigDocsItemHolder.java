package io.quarkus.annotation.processor.generate_doc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final public class ScannedConfigDocsItemHolder {
    private final Map<String, List<ConfigDocItem>> allConfigItemsPerExtension = new HashMap<>();
    private final Map<String, List<ConfigDocItem>> configGroupConfigItems = new HashMap<>();

    public ScannedConfigDocsItemHolder() {
    }

    public Map<String, List<ConfigDocItem>> getAllConfigItemsPerExtension() {
        return allConfigItemsPerExtension;
    }

    public Map<String, List<ConfigDocItem>> getConfigGroupConfigItems() {
        return configGroupConfigItems;
    }

    public void addToAllConfigItems(String configRootName, List<ConfigDocItem> configDocItems) {
        allConfigItemsPerExtension.put(configRootName, configDocItems);
    }

    public void addConfigGroupItems(String configGroupName, List<ConfigDocItem> configDocItems) {
        configGroupConfigItems.put(configGroupName, configDocItems);
    }

    public boolean isEmpty() {
        return allConfigItemsPerExtension.isEmpty();
    }
}
