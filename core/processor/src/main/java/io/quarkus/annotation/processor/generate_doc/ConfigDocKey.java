package io.quarkus.annotation.processor.generate_doc;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

import io.quarkus.annotation.processor.Constants;

final public class ConfigDocKey implements ConfigDocElement, Comparable<ConfigDocElement> {
    private String type;
    private String key;
    private String configDoc;
    private boolean withinAMap;
    private String defaultValue;
    private String javaDocSiteLink;
    private ConfigPhase configPhase;
    private List<String> acceptedValues;

    public ConfigDocKey() {
    }

    public boolean hasType() {
        return type != null && !type.isEmpty();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean hasAcceptedValues() {
        return acceptedValues != null && !acceptedValues.isEmpty();
    }

    public List<String> getAcceptedValues() {
        return acceptedValues;
    }

    public void setAcceptedValues(List<String> acceptedValues) {
        this.acceptedValues = acceptedValues;
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
        ConfigDocKey that = (ConfigDocKey) o;
        return withinAMap == that.withinAMap &&
                Objects.equals(type, that.type) &&
                Objects.equals(key, that.key) &&
                Objects.equals(configDoc, that.configDoc) &&
                Objects.equals(defaultValue, that.defaultValue) &&
                Objects.equals(javaDocSiteLink, that.javaDocSiteLink) &&
                configPhase == that.configPhase &&
                Objects.equals(acceptedValues, that.acceptedValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, key, configDoc, withinAMap, defaultValue, javaDocSiteLink, configPhase, acceptedValues);
    }

    @Override
    public String toString() {
        return "ConfigDocKey{" +
                "type='" + type + '\'' +
                ", key='" + key + '\'' +
                ", configDoc='" + configDoc + '\'' +
                ", withinAMap=" + withinAMap +
                ", defaultValue='" + defaultValue + '\'' +
                ", javaDocSiteLink='" + javaDocSiteLink + '\'' +
                ", configPhase=" + configPhase +
                ", acceptedValues=" + acceptedValues +
                '}';
    }
}
