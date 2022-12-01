package io.quarkus.annotation.processor.generate_doc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ScannedConfigDocsItemHolder {
    private final Map<String, List<ConfigDocItem>> configGroupConfigItems;
    private final Map<ConfigRootInfo, List<ConfigDocItem>> configRootConfigItems;
    private final Map<String, ConfigRootInfo> configRootClassToConfigRootInfo = new HashMap<>();

    public ScannedConfigDocsItemHolder() {
        this(new HashMap<>(), new HashMap<>());
    }

    public ScannedConfigDocsItemHolder(Map<ConfigRootInfo, List<ConfigDocItem>> configRootConfigItems,
            Map<String, List<ConfigDocItem>> configGroupConfigItems) {
        this.configRootConfigItems = configRootConfigItems;
        this.configGroupConfigItems = configGroupConfigItems;
    }

    public Map<String, List<ConfigDocItem>> getConfigGroupConfigItems() {
        return configGroupConfigItems;
    }

    public Map<ConfigRootInfo, List<ConfigDocItem>> getConfigRootConfigItems() {
        return configRootConfigItems;
    }

    public void addConfigGroupItems(String configGroupName, List<ConfigDocItem> configDocItems) {
        configGroupConfigItems.put(configGroupName, configDocItems);
    }

    public void addConfigRootItems(ConfigRootInfo configRoot, List<ConfigDocItem> configDocItems) {
        configRootConfigItems.put(configRoot, configDocItems);
        configRootClassToConfigRootInfo.put(configRoot.getClazz().getQualifiedName().toString(), configRoot);
    }

    public List<ConfigDocItem> getConfigItemsByRootClassName(String configRootClassName) {
        ConfigRootInfo configRootInfo = configRootClassToConfigRootInfo.get(configRootClassName);
        if (configRootInfo == null) {
            return null;
        }

        return configRootConfigItems.get(configRootInfo);
    }

    public boolean isEmpty() {
        return configRootConfigItems.isEmpty();
    }

    @Override
    public String toString() {
        return "ScannedConfigDocsItemHolder{" +
                ", configRootConfigItems=" + configRootConfigItems +
                ", configGroupConfigItems=" + configGroupConfigItems +
                '}';
    }
}
