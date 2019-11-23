package io.quarkus.annotation.processor.generate_doc;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final public class ConfigDocSection implements ConfigDocElement, Comparable<ConfigDocElement> {
    private String name;
    private boolean optional;
    private boolean withinAMap;
    private String sectionDetails;
    private String sectionDetailsTitle;
    private ConfigPhase configPhase;

    private List<ConfigDocItem> configDocItems = new ArrayList<>();
    private String anchorPrefix;

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

    public String getSectionDetailsTitle() {
        return sectionDetailsTitle;
    }

    public void setSectionDetailsTitle(String sectionDetailsTitle) {
        this.sectionDetailsTitle = sectionDetailsTitle;
    }

    public List<ConfigDocItem> getConfigDocItems() {
        return configDocItems;
    }

    public void setConfigDocItems(List<ConfigDocItem> configDocItems) {
        this.configDocItems = configDocItems;
    }

    public void addConfigDocItems(List<ConfigDocItem> configDocItems) {
        this.configDocItems.addAll(configDocItems);
    }

    @Override
    public void accept(Writer writer, DocFormatter docFormatter) throws IOException {
        docFormatter.format(writer, this);
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
        return sectionDetailsTitle.equals(that.sectionDetailsTitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sectionDetailsTitle);
    }

    @Override
    public String toString() {
        return "ConfigDocSection{" +
                "name='" + name + '\'' +
                ", optional='" + optional + '\'' +
                ", withinAMap=" + withinAMap +
                ", sectionDetails='" + sectionDetails + '\'' +
                ", sectionDetailsTitle='" + sectionDetailsTitle + '\'' +
                ", configPhase=" + configPhase +
                ", configDocItems=" + configDocItems +
                ", anchorPrefix='" + anchorPrefix + '\'' +
                '}';
    }

    public boolean hasDurationInformationNote() {
        for (ConfigDocItem item : configDocItems) {
            if (item.hasDurationInformationNote())
                return true;
        }
        return false;
    }

    public boolean hasMemoryInformationNote() {
        for (ConfigDocItem item : configDocItems) {
            if (item.hasMemoryInformationNote())
                return true;
        }
        return false;
    }

    public void setAnchorPrefix(String anchorPrefix) {
        this.anchorPrefix = anchorPrefix;
    }

    public String getAnchorPrefix() {
        return anchorPrefix;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }
}
