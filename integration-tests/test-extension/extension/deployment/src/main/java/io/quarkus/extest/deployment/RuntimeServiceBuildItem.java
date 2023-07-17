package io.quarkus.extest.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.extest.runtime.RuntimeXmlConfigService;
import io.quarkus.runtime.RuntimeValue;

final public class RuntimeServiceBuildItem extends SimpleBuildItem {
    private RuntimeValue<RuntimeXmlConfigService> service;

    public RuntimeServiceBuildItem(RuntimeValue<RuntimeXmlConfigService> service) {
        this.service = service;
    }

    public RuntimeValue<RuntimeXmlConfigService> getService() {
        return service;
    }
}
