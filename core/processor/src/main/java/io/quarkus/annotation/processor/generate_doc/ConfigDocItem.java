package io.quarkus.annotation.processor.generate_doc;

import java.io.IOException;
import java.io.Writer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A config doc item is either a config section {@link ConfigDocSection} or a config key {@link ConfigDocKey}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
final public class ConfigDocItem implements ConfigDocElement, Comparable<ConfigDocItem> {
    private ConfigDocKey configDocKey;
    private ConfigDocSection configDocSection;

    public ConfigDocItem() {
    }

    public ConfigDocItem(ConfigDocSection configDocSection, ConfigDocKey configDocKey) {
        this.configDocSection = configDocSection;
        this.configDocKey = configDocKey;
    }

    public ConfigDocKey getConfigDocKey() {
        return configDocKey;
    }

    public void setConfigDocKey(ConfigDocKey configDocKey) {
        this.configDocKey = configDocKey;
    }

    public ConfigDocSection getConfigDocSection() {
        return configDocSection;
    }

    public void setConfigDocSection(ConfigDocSection configDocSection) {
        this.configDocSection = configDocSection;
    }

    @JsonIgnore
    boolean isConfigSection() {
        return configDocSection != null;
    }

    @JsonIgnore
    boolean isConfigKey() {
        return configDocKey != null;
    }

    @Override
    public String toString() {
        return "ConfigDocItem{" +
                "configDocSection=" + configDocSection +
                ", configDocKey=" + configDocKey +
                '}';
    }

    @Override
    public void accept(Writer writer, DocFormatter docFormatter) throws IOException {
        if (isConfigSection()) {
            configDocSection.accept(writer, docFormatter);
        } else if (isConfigKey()) {
            configDocKey.accept(writer, docFormatter);
        }
    }

    @JsonIgnore
    @Override
    public ConfigPhase getConfigPhase() {
        if (isConfigSection()) {
            return configDocSection.getConfigPhase();
        } else if (isConfigKey()) {
            return configDocKey.getConfigPhase();
        }

        return null;
    }

    @JsonIgnore
    @Override
    public boolean isWithinAMap() {
        if (isConfigSection()) {
            return configDocSection.isWithinAMap();
        } else if (isConfigKey()) {
            return configDocKey.isWithinAMap();
        }

        return false;
    }

    @Override
    @JsonIgnore
    public String getTopLevelGrouping() {
        if (isConfigKey()) {
            return configDocKey.getTopLevelGrouping();
        } else if (isConfigSection()) {
            return configDocSection.getTopLevelGrouping();
        }

        return null;
    }

    @JsonIgnore
    public boolean isWithinAConfigGroup() {
        if (isConfigSection()) {
            return true;
        } else if (isConfigKey() && configDocKey.isWithinAConfigGroup()) {
            return true;
        }

        return false;
    }

    /**
     * TODO determine section ordering
     *
     * @param item
     * @return
     */
    @Override
    public int compareTo(ConfigDocItem item) {
        // ensure that different config objects in the same extension don't cross streams
        if (isConfigKey() && item.isConfigKey() && (!getTopLevelGrouping().equals(item.getTopLevelGrouping()))) {
            return getTopLevelGrouping().compareTo(item.getTopLevelGrouping());
        }

        if (isConfigSection() && item.isConfigKey()) {
            return 1; // push sections to the end of the list
        } else if (isConfigKey() && item.isConfigSection()) {
            return -1; // push section to the end of the list
        }

        return compare(item);
    }

    public boolean hasDurationInformationNote() {
        if (isConfigKey()) {
            return DocGeneratorUtil.hasDurationInformationNote(configDocKey);
        } else if (isConfigSection()) {
            return configDocSection.hasDurationInformationNote();
        }
        return false;
    }

    public boolean hasMemoryInformationNote() {
        if (isConfigKey()) {
            return DocGeneratorUtil.hasMemoryInformationNote(configDocKey);
        } else if (isConfigSection()) {
            return configDocSection.hasMemoryInformationNote();
        }
        return false;
    }

    public void configPhase(ConfigPhase phase) {
        if (isConfigKey()) {
            configDocKey.setConfigPhase(phase);
        } else {
            configDocSection.setConfigPhase(phase);
        }
    }

    public void withinAMap(boolean withinAMap) {
        if (isConfigKey()) {
            configDocKey.setWithinAMap(configDocKey.isWithinAMap() || withinAMap);
        } else {
            configDocSection.setWithinAMap(configDocSection.isWithinAMap() || withinAMap);
        }
    }
}
