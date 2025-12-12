package io.quarkus.test.component;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

class QuarkusComponentTestConfigSource implements ConfigSource {

    private final Map<String, String> configProperties;
    private final int ordinal;

    QuarkusComponentTestConfigSource(Map<String, String> configProperties, int ordinal) {
        this.configProperties = configProperties;
        this.ordinal = ordinal;
    }

    @Override
    public Set<String> getPropertyNames() {
        return configProperties.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return configProperties.get(propertyName);
    }

    @Override
    public String getName() {
        return QuarkusComponentTestExtension.class.getName();
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

}
