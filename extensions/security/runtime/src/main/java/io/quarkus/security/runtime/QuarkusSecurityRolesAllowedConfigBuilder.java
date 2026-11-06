package io.quarkus.security.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public final class QuarkusSecurityRolesAllowedConfigBuilder implements ConfigBuilder {

    private static final String ROLES_ALLOWED_CONFIG_SOURCE = "QuarkusSecurityRolesAllowedConfigSource";
    private static volatile Map<String, String> properties = Map.of();
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

    /**
     * @param configExpressions configuration expressions ordered by their artificial keys, that is, the key
     *        of an expression is its index in the list
     */
    public static void setProperties(List<String> configExpressions) {
        Map<String, String> result = new HashMap<>(configExpressions.size());
        for (int i = 0; i < configExpressions.size(); i++) {
            result.put(transformToKey(i), configExpressions.get(i));
        }
        properties = Map.copyOf(result);
    }

    static String transformToKey(int i) {
        return ROLES_ALLOWED_CONFIG_SOURCE + ".property-" + i;
    }
}
