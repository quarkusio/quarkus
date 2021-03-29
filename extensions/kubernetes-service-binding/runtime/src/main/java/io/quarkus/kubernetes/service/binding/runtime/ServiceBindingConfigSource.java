package io.quarkus.kubernetes.service.binding.runtime;

import java.util.Map;

import io.smallrye.config.common.MapBackedConfigSource;

public class ServiceBindingConfigSource extends MapBackedConfigSource {

    public ServiceBindingConfigSource(String name, Map<String, String> propertyMap) {
        super(name, propertyMap, 270, false);
    }
}
