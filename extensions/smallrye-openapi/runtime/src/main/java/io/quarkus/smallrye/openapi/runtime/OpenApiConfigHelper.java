package io.quarkus.smallrye.openapi.runtime;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

import org.eclipse.microprofile.config.Config;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.AbstractConfigSource;

public class OpenApiConfigHelper {
    public static Config wrap(Config config, String documentName) {
        SmallRyeConfigBuilder configBuilder = new SmallRyeConfigBuilder();
        configBuilder
                .withInterceptorFactories(new ConfigSourceInterceptorFactory() {
                    @Override
                    public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
                        return new OpenApiConfigMapping(documentName);
                    }

                    @Override
                    public OptionalInt getPriority() {
                        return OptionalInt.of(Priorities.LIBRARY + 300);
                    }
                })
                .withInterceptorFactories(new DelegatingConfigInterceptor(config));
        configBuilder.withSources(new AbstractConfigSource("OpenApiConfigHelper", 1) {
            @Override
            public Set<String> getPropertyNames() {
                Set<String> propertyNames = new HashSet<>();
                for (String propertyName : config.getPropertyNames()) {
                    propertyNames.add(propertyName);
                }
                return propertyNames;
            }

            @Override
            public String getValue(String propertyName) {
                return null;
            }
        });

        return configBuilder.build();
    }

    public static class DelegatingConfigInterceptor implements ConfigSourceInterceptorFactory {
        private final Config config;

        public DelegatingConfigInterceptor(Config config) {
            this.config = config;
        }

        @Override
        public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
            return new ConfigSourceInterceptor() {
                @Override
                public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
                    return (ConfigValue) config.getConfigValue(name);
                }
            };
        }

        @Override
        public OptionalInt getPriority() {
            return OptionalInt.of(Priorities.LIBRARY + 200);
        }
    }
}
