package io.quarkus.spring.di.deployment;

import java.util.Map;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The purpose of this bean is to map the names of the Spring Beans to their associated DotName
 * This info is needed when trying to convert SpEL expressions that reference beans by name, to bytecode
 */
public final class SpringBeanNameToDotNameBuildItem extends SimpleBuildItem {

    private final Map<String, DotName> map;

    public SpringBeanNameToDotNameBuildItem(Map<String, DotName> map) {
        this.map = map;
    }

    public Map<String, DotName> getMap() {
        return map;
    }
}
