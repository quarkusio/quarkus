package io.quarkus.infinispan.client.deployment;

import java.util.Map;
import java.util.Properties;

import io.quarkus.builder.item.SimpleBuildItem;

public final class MarshallingBuildItem extends SimpleBuildItem {

    // holds protostream requirements
    private final Properties properties;

    // a marshaller can be defined for different client names
    private Map<String, Object> marshallers;

    public MarshallingBuildItem(Properties properties, Map<String, Object> marshallers) {
        this.properties = properties;
        this.marshallers = marshallers;
    }

    public Object getMarshallerForClientName(String clientName) {
        return marshallers.get(clientName);
    }

    public Properties getProperties() {
        return properties;
    }
}
