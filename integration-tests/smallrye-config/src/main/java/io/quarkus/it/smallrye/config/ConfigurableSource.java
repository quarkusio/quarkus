package io.quarkus.it.smallrye.config;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;

public class ConfigurableSource implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        final ConfigValue value = context.getValue("configurable.source.init");
        if (value != null && "database".equals(value.getValue())) {
            return singletonList(new DatabaseConfigSource());
        }
        return emptyList();
    }

    public static class DatabaseConfigSource implements ConfigSource {
        private final Map<String, String> values = new HashMap<>();

        public DatabaseConfigSource() {
            values.put("database.user.naruto", "uzumaki");
            values.put("database.user.sasuke", "uchiha");
        }

        @Override
        public Set<String> getPropertyNames() {
            return values.keySet();
        }

        @Override
        public String getValue(final String propertyName) {
            return values.get(propertyName);
        }

        @Override
        public String getName() {
            return "database";
        }

        @Override
        public int getOrdinal() {
            return 150;
        }
    }
}
