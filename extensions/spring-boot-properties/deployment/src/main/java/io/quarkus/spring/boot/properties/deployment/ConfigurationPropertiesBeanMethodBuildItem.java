package io.quarkus.spring.boot.properties.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class ConfigurationPropertiesBeanMethodBuildItem extends MultiBuildItem {
    private final ClassInfo returnType;
    private final String prefix;

    public ConfigurationPropertiesBeanMethodBuildItem(ClassInfo returnType, String prefix) {
        this.returnType = returnType;
        this.prefix = prefix;
    }

    public ClassInfo getReturnType() {
        return returnType;
    }

    public DotName getReturnTypeName() {
        return returnType.name();
    }

    public String getPrefix() {
        return prefix;
    }
}
