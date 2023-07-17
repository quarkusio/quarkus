package io.quarkus.qute.deployment;

import java.util.Collection;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.SimpleBuildItem;

final class EngineConfigurationsBuildItem extends SimpleBuildItem {

    private final Collection<ClassInfo> configurations;

    EngineConfigurationsBuildItem(Collection<ClassInfo> configurations) {
        this.configurations = configurations;
    }

    public Collection<ClassInfo> getConfigurations() {
        return configurations;
    }

}
