package io.quarkus.annotation.processor.generate_doc;

import java.util.Objects;

import javax.lang.model.element.TypeElement;

final public class ConfigRootInfo {
    private final String name;
    private final TypeElement clazz;
    private final ConfigPhase configPhase;
    private final String extensionName;

    public ConfigRootInfo(String name, TypeElement clazz, String extensionName, ConfigPhase visibility) {
        this.name = name;
        this.clazz = clazz;
        this.configPhase = visibility;
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
                Objects.equals(configPhase, that.configPhase) &&
                Objects.equals(extensionName, that.extensionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, clazz, configPhase, extensionName);
    }

    @Override
    public String toString() {
        return "ConfigRootInfo{" +
                "name='" + name + '\'' +
                ", clazz=" + clazz +
                ", configPhase=" + configPhase +
                ", extensionName='" + extensionName + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public TypeElement getClazz() {
        return clazz;
    }

    public ConfigPhase getConfigPhase() {
        return configPhase;
    }
}
