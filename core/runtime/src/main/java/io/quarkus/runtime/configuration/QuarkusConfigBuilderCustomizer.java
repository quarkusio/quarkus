package io.quarkus.runtime.configuration;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOG_VALUES;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;

import java.util.Iterator;
import java.util.OptionalInt;
import java.util.function.Function;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.NameIterator;
import io.smallrye.config.Priorities;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class QuarkusConfigBuilderCustomizer implements SmallRyeConfigBuilderCustomizer {
    public static final String QUARKUS_PROFILE = "quarkus.profile";
    public static final String QUARKUS_CONFIG_LOCATIONS = "quarkus.config.locations";
    public static final String QUARKUS_CONFIG_PROFILE_PARENT = "quarkus.config.profile.parent";
    public static final String QUARKUS_CONFIG_MAPPING_VALIDATE_UNKNOWN = "quarkus.config.mapping.validate-unknown";
    public static final String QUARKUS_CONFIG_LOG_VALUES = "quarkus.config.log.values";

    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        LaunchMode launchMode = LaunchMode.current();
        builder.withDefaultValue(launchMode.getProfileKey(), launchMode.getDefaultProfile());

        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new RelocateConfigSourceInterceptor(new Function<String, String>() {
                    @Override
                    public String apply(final String name) {
                        return SMALLRYE_CONFIG_PROFILE.equals(name) ? launchMode.getProfileKey() : name;
                    };
                });
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 200 - 10);
            }
        });

        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                // Also adds relocations to all profiles
                return new RelocateConfigSourceInterceptor(new Function<String, String>() {
                    @Override
                    public String apply(final String name) {
                        if (SMALLRYE_CONFIG_LOCATIONS.equals(name)) {
                            return QUARKUS_CONFIG_LOCATIONS;
                        }
                        if (SMALLRYE_CONFIG_PROFILE_PARENT.equals(name)) {
                            return QUARKUS_CONFIG_PROFILE_PARENT;
                        }
                        if (SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN.equals(name)) {
                            return QUARKUS_CONFIG_MAPPING_VALIDATE_UNKNOWN;
                        }
                        if (SMALLRYE_CONFIG_LOG_VALUES.equals(name)) {
                            return QUARKUS_CONFIG_LOG_VALUES;
                        }

                        if (name.startsWith("%") && name.endsWith(SMALLRYE_CONFIG_LOCATIONS)) {
                            io.smallrye.config.NameIterator ni = new io.smallrye.config.NameIterator(name);
                            return ni.getNextSegment() + "." + QUARKUS_CONFIG_LOCATIONS;
                        }

                        if (name.startsWith("%") && name.endsWith(SMALLRYE_CONFIG_PROFILE_PARENT)) {
                            io.smallrye.config.NameIterator ni = new NameIterator(name);
                            return ni.getNextSegment() + "." + QUARKUS_CONFIG_PROFILE_PARENT;
                        }

                        return name;
                    }
                }) {
                    @Override
                    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
                        return context.iterateNames();
                    }
                };
            }

            @Override
            public OptionalInt getPriority() {
                // So it executes before the ProfileConfigSourceInterceptor and applies the profile relocation
                return OptionalInt.of(Priorities.LIBRARY + 200 - 5);
            }
        });

        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new FallbackConfigSourceInterceptor(new Function<String, String>() {
                    @Override
                    public String apply(final String name) {
                        if (QUARKUS_PROFILE.equals(name)) {
                            return SMALLRYE_CONFIG_PROFILE;
                        }
                        if (QUARKUS_CONFIG_LOCATIONS.equals(name)) {
                            return SMALLRYE_CONFIG_LOCATIONS;
                        }
                        if (QUARKUS_CONFIG_PROFILE_PARENT.equals(name)) {
                            return SMALLRYE_CONFIG_PROFILE_PARENT;
                        }
                        if (QUARKUS_CONFIG_MAPPING_VALIDATE_UNKNOWN.equals(name)) {
                            return SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN;
                        }
                        if (QUARKUS_CONFIG_LOG_VALUES.equals(name)) {
                            return SMALLRYE_CONFIG_LOG_VALUES;
                        }
                        return name;
                    }
                }) {
                    @Override
                    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
                        return context.iterateNames();
                    }
                };
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 600 - 5);
            }
        });

        // Ignore unmapped quarkus properties, because properties in the same root may be split between build / runtime
        builder.withMappingIgnore("quarkus.**");
    }
}
