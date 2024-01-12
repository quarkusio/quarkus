package io.quarkus.runtime.configuration;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOG_VALUES;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;

import java.util.HashMap;
import java.util.Map;
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
    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        LaunchMode launchMode = ProfileManager.getLaunchMode();
        builder.withDefaultValue(launchMode.getProfileKey(), launchMode.getDefaultProfile());

        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new RelocateConfigSourceInterceptor(Map.of(SMALLRYE_CONFIG_PROFILE, launchMode.getProfileKey()));
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 200 - 10);
            }
        });

        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                Map<String, String> relocations = new HashMap<>();
                relocations.put(SMALLRYE_CONFIG_LOCATIONS, "quarkus.config.locations");
                relocations.put(SMALLRYE_CONFIG_PROFILE_PARENT, "quarkus.config.profile.parent");
                relocations.put(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, "quarkus.config.mapping.validate-unknown");
                relocations.put(SMALLRYE_CONFIG_LOG_VALUES, "quarkus.config.log.values");

                // Also adds relocations to all profiles
                return new RelocateConfigSourceInterceptor(new Function<String, String>() {
                    @Override
                    public String apply(final String name) {
                        String relocate = relocations.get(name);
                        if (relocate != null) {
                            return relocate;
                        }

                        if (name.startsWith("%") && name.endsWith(SMALLRYE_CONFIG_LOCATIONS)) {
                            io.smallrye.config.NameIterator ni = new io.smallrye.config.NameIterator(name);
                            return ni.getNextSegment() + "." + "quarkus.config.locations";
                        }

                        if (name.startsWith("%") && name.endsWith(SMALLRYE_CONFIG_PROFILE_PARENT)) {
                            io.smallrye.config.NameIterator ni = new NameIterator(name);
                            return ni.getNextSegment() + "." + "quarkus.config.profile.parent";
                        }

                        return name;
                    }
                });
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
                Map<String, String> fallbacks = new HashMap<>();
                fallbacks.put("quarkus.profile", SMALLRYE_CONFIG_PROFILE);
                fallbacks.put("quarkus.config.locations", SMALLRYE_CONFIG_LOCATIONS);
                fallbacks.put("quarkus.config.profile.parent", SMALLRYE_CONFIG_PROFILE_PARENT);
                fallbacks.put("quarkus.config.mapping.validate-unknown", SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN);
                fallbacks.put("quarkus.config.log.values", SMALLRYE_CONFIG_LOG_VALUES);
                return new FallbackConfigSourceInterceptor(fallbacks);
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
