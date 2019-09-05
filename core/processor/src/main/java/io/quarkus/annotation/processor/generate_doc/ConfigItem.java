package io.quarkus.annotation.processor.generate_doc;

import java.util.Objects;
import java.util.regex.Matcher;

import io.quarkus.annotation.processor.Constants;

final public class ConfigItem implements Comparable<ConfigItem> {
    private String type;
    private String key;
    private String configDoc;
    private boolean withinAMap;
    private String defaultValue;
    private ConfigPhase configPhase;
    private String javaDocSiteLink;

    public ConfigItem() {
    }

    @Override
    public String toString() {
        return "ConfigItem{" +
                "type='" + type + '\'' +
                ", key='" + key + '\'' +
                ", configDoc='" + configDoc + '\'' +
                ", withinAMap=" + withinAMap +
                ", defaultValue='" + defaultValue + '\'' +
                ", configPhase=" + configPhase +
                ", javaDocSiteLink='" + javaDocSiteLink + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ConfigItem that = (ConfigItem) o;
        return withinAMap == that.withinAMap &&
                Objects.equals(type, that.type) &&
                Objects.equals(key, that.key) &&
                Objects.equals(configDoc, that.configDoc) &&
                Objects.equals(defaultValue, that.defaultValue) &&
                configPhase == that.configPhase &&
                Objects.equals(javaDocSiteLink, that.javaDocSiteLink);
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, key, configDoc, withinAMap, defaultValue, configPhase, javaDocSiteLink);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getConfigDoc() {
        return configDoc;
    }

    public void setConfigDoc(String configDoc) {
        this.configDoc = configDoc;
    }

    public String getJavaDocSiteLink() {
        if (javaDocSiteLink == null) {
            return Constants.EMPTY;
        }

        return javaDocSiteLink;
    }

    public void setJavaDocSiteLink(String javaDocSiteLink) {
        this.javaDocSiteLink = javaDocSiteLink;
    }

    public String getDefaultValue() {
        if (!defaultValue.isEmpty()) {
            return defaultValue;
        }

        final String defaultValue = DocGeneratorUtil.getPrimitiveDefaultValue(type);

        if (defaultValue == null) {
            return Constants.EMPTY;
        }

        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public ConfigPhase getConfigPhase() {
        return configPhase;
    }

    public void setConfigPhase(ConfigPhase configPhase) {
        this.configPhase = configPhase;
    }

    public void setWithinAMap(boolean withinAMap) {
        this.withinAMap = withinAMap;
    }

    @SuppressWarnings("unused")
    public boolean isWithinAMap() {
        return withinAMap;
    }

    String computeTypeSimpleName() {
        String unwrappedType = DocGeneratorUtil.unbox(type);

        Matcher matcher = Constants.CLASS_NAME_PATTERN.matcher(unwrappedType);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return unwrappedType;
    }

    /**
     *
     * Map config will be at the end of generated doc.
     * Order build time config first
     * Otherwise maintain source code order.
     */
    @Override
    public int compareTo(ConfigItem item) {
        if (withinAMap) {
            if (item.withinAMap) {
                return 0;
            }
            return 1;
        } else if (item.withinAMap) {
            return -1;
        }

        int phaseComparison = ConfigPhase.COMPARATOR.compare(this.configPhase, item.configPhase);
        if (phaseComparison == 0) {
            return 0;
        }

        return phaseComparison;
    }
}
