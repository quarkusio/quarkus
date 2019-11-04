package io.quarkus.annotation.processor.generate_doc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ScannedConfigDocsItemHolder {
    private final Map<String, List<ConfigDocItem>> generalConfigItems;
    private final Map<String, List<ConfigDocItem>> configRootConfigItems;
    private final Map<String, List<ConfigDocItem>> configGroupConfigItems;

    public ScannedConfigDocsItemHolder() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public ScannedConfigDocsItemHolder(Map<String, List<ConfigDocItem>> configRootConfigItems,
            Map<String, List<ConfigDocItem>> configGroupConfigItems, Map<String, List<ConfigDocItem>> generalConfigItems) {
        this.configRootConfigItems = configRootConfigItems;
        this.configGroupConfigItems = configGroupConfigItems;
        this.generalConfigItems = generalConfigItems;
    }

    public Map<String, List<ConfigDocItem>> getConfigGroupConfigItems() {
        return configGroupConfigItems;
    }

    public Map<String, List<ConfigDocItem>> getConfigRootConfigItems() {
        return configRootConfigItems;
    }

    public Map<String, List<ConfigDocItem>> getGeneralConfigItems() {
        return generalConfigItems;
    }

    public void addConfigGroupItems(String configGroupName, List<ConfigDocItem> configDocItems) {
        configGroupConfigItems.put(configGroupName, configDocItems);
    }

    public void addConfigRootItems(String configRoot, List<ConfigDocItem> configDocItems) {
        configRootConfigItems.put(configRoot, configDocItems);
    }

    public void addGeneralConfigItems(String configRoot, List<ConfigDocItem> configDocItems) {
        configRootConfigItems.put(configRoot, configDocItems);
    }

    public boolean isEmpty() {
        return configRootConfigItems.isEmpty();
    }
}
