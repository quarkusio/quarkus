package io.quarkus.annotation.processor.generate_doc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final public class ConfigDocSection implements ConfigDocElement, Comparable<ConfigDocElement> {
    private String name;
    private boolean withinAMap;
    private String sectionDetails;
    private ConfigPhase configPhase;
    private List<ConfigDocItem> configDocItems = new ArrayList<>();

    public ConfigDocSection() {
    }

    public boolean isWithinAMap() {
        return withinAMap;
    }

    public void setWithinAMap(boolean withinAMap) {
        this.withinAMap = withinAMap;
    }

    public ConfigPhase getConfigPhase() {
        return configPhase;
    }

    public void setConfigPhase(ConfigPhase configPhase) {
        this.configPhase = configPhase;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSectionDetails() {
        return sectionDetails;
    }

    public void setSectionDetails(String sectionDetails) {
        this.sectionDetails = sectionDetails;
    }

    public List<ConfigDocItem> getConfigDocItems() {
        return configDocItems;
    }

    public void setConfigDocItems(List<ConfigDocItem> configDocItems) {
        this.configDocItems = configDocItems;
    }

    @Override
    public String accept(DocFormatter docFormatter) {
        return docFormatter.format(this);
    }

    @Override
    public int compareTo(ConfigDocElement o) {
        return compare(o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ConfigDocSection that = (ConfigDocSection) o;
        return withinAMap == that.withinAMap &&
                Objects.equals(name, that.name) &&
                Objects.equals(sectionDetails, that.sectionDetails) &&
                configPhase == that.configPhase &&
                Objects.equals(configDocItems, that.configDocItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, withinAMap, sectionDetails, configPhase, configDocItems);
    }

    @Override
    public String toString() {
        return "ConfigDocSection{" +
                ", name='" + name + '\'' +
                ", withinAMap=" + withinAMap +
                ", sectionDetails='" + sectionDetails + '\'' +
                ", configPhase=" + configPhase +
                ", configDocItems=" + configDocItems +
                '}';
    }
}
