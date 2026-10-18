package io.quarkus.jackson.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * BuildItem used to signal that some Jackson module has been detected on the classpath
 *
 * The modules are then registered with the ObjectMapper.
 *
 * Note: Modules are assumed to have a default constructor
 */
public final class ClassPathJacksonModuleBuildItem extends MultiBuildItem {

    private final String moduleClassName;

    public ClassPathJacksonModuleBuildItem(String moduleClassName) {
        this.moduleClassName = moduleClassName;
    }

    public String getModuleClassName() {
        return moduleClassName;
    }
}
