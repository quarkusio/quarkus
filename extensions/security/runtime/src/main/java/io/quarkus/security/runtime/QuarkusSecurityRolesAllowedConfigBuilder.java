package io.quarkus.security.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public final class QuarkusSecurityRolesAllowedConfigBuilder implements ConfigBuilder {

    private static final String ROLES_ALLOWED_CONFIG_SOURCE = "QuarkusSecurityRolesAllowedConfigSource";
    private static final Map<String, String> properties = new HashMap<>();
    private final ConfigSource configSource = new ConfigSource() {
        @Override
        public Set<String> getPropertyNames() {
            return properties.keySet();
        }

        @Override
        public String getValue(String key) {
            return properties.get(key);
        }

        @Override
        public String getName() {
            return ROLES_ALLOWED_CONFIG_SOURCE;
        }
    };

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        builder.getSources().add(configSource);
        return builder;
    }

    static void addProperty(int key, String value) {
        // this method should be called during static init
        properties.put(transformToKey(key), value);
    }

    static String transformToKey(int i) {
        return ROLES_ALLOWED_CONFIG_SOURCE + ".property-" + i;
    }
}
