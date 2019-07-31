package io.quarkus.annotation.processor.generate_doc;

import java.util.Objects;

import javax.lang.model.element.TypeElement;

import io.quarkus.annotation.processor.Constants;

public class ConfigRootInfo {
    private final String name;
    private final TypeElement clazz;
    private final ConfigVisibility visibility;
    private final String extensionName;

    public ConfigRootInfo(String name, TypeElement clazz, String extensionName, ConfigVisibility visibility) {
        this.name = name;
        this.clazz = clazz;
        this.visibility = visibility;
        this.extensionName = extensionName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ConfigRootInfo that = (ConfigRootInfo) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(clazz, that.clazz) &&
                Objects.equals(visibility, that.visibility) &&
                Objects.equals(extensionName, that.extensionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, clazz, visibility, extensionName);
    }

    @Override
    public String toString() {
        return "ConfigRootInfo{" +
                "name='" + name + '\'' +
                ", clazz=" + clazz +
                ", visibility='" + visibility + '\'' +
                ", extensionName='" + extensionName + '\'' +
                '}';
    }

    public String getConfigRootId() {
        if (name.contains(".datasource")) {
            return String.format("%s-%s", extensionName, name.replaceAll(Constants.QUARKUS, "")).replaceAll("\\.", "-");
        }

        return name.replaceAll("\\.", "-");
    }

    public String getName() {
        return name;
    }

    public TypeElement getClazz() {
        return clazz;
    }

    public ConfigVisibility getVisibility() {
        return visibility;
    }
}
