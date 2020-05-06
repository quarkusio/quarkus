package io.quarkus.annotation.processor.generate_doc;

import java.util.Objects;

import javax.lang.model.element.TypeElement;

final public class ConfigRootInfo {
    private final String name;
    private final TypeElement clazz;
    private final ConfigPhase configPhase;
    private final String fileName;

    public ConfigRootInfo(String name, TypeElement clazz, ConfigPhase visibility, String fileName) {
        this.name = name;
        this.clazz = clazz;
        this.configPhase = visibility;
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
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
                configPhase == that.configPhase &&
                Objects.equals(fileName, that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, clazz, configPhase, fileName);
    }

    @Override
    public String toString() {
        return "ConfigRootInfo{" +
                "name='" + name + '\'' +
                ", clazz=" + clazz +
                ", configPhase=" + configPhase +
                ", fileName='" + fileName + '\'' +
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
