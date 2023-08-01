package io.quarkus.runtime.configuration;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.DotEnvConfigSourceProvider;
import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.NameIterator;
import io.smallrye.config.Priorities;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 *
 */
public final class ConfigUtils {

    /**
     * The name of the property associated with a random UUID generated at launch time.
     */
    static final String UUID_KEY = "quarkus.uuid";

    private ConfigUtils() {
    }

    public static <T> IntFunction<List<T>> listFactory() {
        return ArrayList::new;
    }

    public static <T> IntFunction<Set<T>> setFactory() {
        return LinkedHashSet::new;
    }

    public static <T> IntFunction<SortedSet<T>> sortedSetFactory() {
        return size -> new TreeSet<>();
    }

    public static SmallRyeConfigBuilder configBuilder(final boolean runTime, final LaunchMode launchMode) {
        return configBuilder(runTime, true, launchMode);
    }

    /**
     * Get the basic configuration builder.
     *
     * @param runTime {@code true} if the configuration is run time, {@code false} if build time
     * @param addDiscovered {@code true} if the ConfigSource and Converter objects should be auto-discovered
     * @return the configuration builder
     */
    public static SmallRyeConfigBuilder configBuilder(final boolean runTime, final boolean addDiscovered,
            final LaunchMode launchMode) {
        SmallRyeConfigBuilder builder = emptyConfigBuilder();

        if (launchMode.isDevOrTest() && runTime) {
            builder.withSources(new RuntimeOverrideConfigSource(Thread.currentThread().getContextClassLoader()));
        }
        if (runTime) {
            builder.withDefaultValue(UUID_KEY, UUID.randomUUID().toString());
        }
        if (addDiscovered) {
            builder.addDiscoveredSources();
        }
        return builder;
    }

    public static SmallRyeConfigBuilder emptyConfigBuilder() {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
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
                return new FallbackConfigSourceInterceptor(fallbacks);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 600 - 5);
            }
        });

        // Ignore unmapped quarkus properties, because properties in the same root may be split between build / runtime
        builder.withMappingIgnore("quarkus.**");

        builder.forClassLoader(Thread.currentThread().getContextClassLoader())
                .addDiscoveredConverters()
                .addDefaultInterceptors()
                .addDiscoveredInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .addDefaultSources()
                .withSources(new ApplicationPropertiesConfigSourceLoader.InFileSystem())
                .withSources(new ApplicationPropertiesConfigSourceLoader.InClassPath())
                .withSources(new DotEnvConfigSourceProvider());

        return builder;
    }

    @SuppressWarnings("unchecked")
    public static SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder, List<String> configBuildersNames) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            List<ConfigBuilder> configBuilders = new ArrayList<>();
            for (String configBuilderName : configBuildersNames) {
                Class<ConfigBuilder> configBuilderClass = (Class<ConfigBuilder>) contextClassLoader
                        .loadClass(configBuilderName);
                configBuilders.add(configBuilderClass.getDeclaredConstructor().newInstance());
            }
            configBuilders.sort(ConfigBuilderComparator.INSTANCE);

            for (ConfigBuilder configBuilder : configBuilders) {
                builder = configBuilder.configBuilder(builder);
                if (builder == null) {
                    throw new ConfigurationException(configBuilder.getClass().getName() + " returned a null builder");
                }
            }

        } catch (ClassNotFoundException | InstantiationException | InvocationTargetException | NoSuchMethodException
                | IllegalAccessException e) {
            throw new ConfigurationException(e);
        }
        return builder;
    }

    /**
     * Add a configuration source provider to the builder.
     *
     * @param builder the builder
     * @param provider the provider to add
     */
    public static void addSourceProvider(SmallRyeConfigBuilder builder, ConfigSourceProvider provider) {
        final Iterable<ConfigSource> sources = provider.getConfigSources(Thread.currentThread().getContextClassLoader());
        for (ConfigSource source : sources) {
            builder.withSources(source);
        }
    }

    /**
     * Add a configuration source providers to the builder.
     *
     * @param builder the builder
     * @param providers the providers to add
     */
    public static void addSourceProviders(SmallRyeConfigBuilder builder, Collection<ConfigSourceProvider> providers) {
        for (ConfigSourceProvider provider : providers) {
            addSourceProvider(builder, provider);
        }
    }

    public static void addSourceFactoryProvider(SmallRyeConfigBuilder builder, ConfigSourceFactoryProvider provider) {
        builder.withSources(provider.getConfigSourceFactory(Thread.currentThread().getContextClassLoader()));
    }

    public static List<String> getProfiles() {
        return ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getProfiles();
    }

    public static boolean isProfileActive(final String profile) {
        return getProfiles().contains(profile);
    }

    /**
     * Checks if a property is present in the current Configuration.
     * <p>
     * Because the sources may not expose the property directly in {@link ConfigSource#getPropertyNames()}, we cannot
     * reliably determine if the property is present in the properties list. The property needs to be retrieved to make
     * sure it exists. Also, if the value is an expression, we want to ignore expansion, because this is not relevant
     * for the check and the expansion value may not be available at this point.
     * <p>
     * It may be interesting to expose such API in SmallRyeConfig directly.
     *
     * @param propertyName the property name.
     * @return true if the property is present or false otherwise.
     */
    public static boolean isPropertyPresent(String propertyName) {
        return ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).isPropertyPresent(propertyName);
    }

    /**
     * Checks if any of the given properties is present in the current Configuration.
     * <p>
     * Because the sources may not expose the property directly in {@link ConfigSource#getPropertyNames()}, we cannot
     * reliably determine if the property is present in the properties list. The property needs to be retrieved to make
     * sure it exists. Also, if the value is an expression, we want to ignore expansion, because this is not relevant
     * for the check and the expansion value may not be available at this point.
     * <p>
     * It may be interesting to expose such API in SmallRyeConfig directly.
     *
     * @param propertyNames The configuration property names
     * @return true if the property is present or false otherwise.
     */
    public static boolean isAnyPropertyPresent(Collection<String> propertyNames) {
        for (String propertyName : propertyNames) {
            if (isPropertyPresent(propertyName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the value of the first given property present in the current Configuration,
     * or {@link Optional#empty()} if none of the properties is present.
     *
     * @param <T> The property type
     * @param propertyNames The configuration property names
     * @param propertyType The type that the resolved property value should be converted to
     * @return true if the property is present or false otherwise.
     */
    public static <T> Optional<T> getFirstOptionalValue(List<String> propertyNames, Class<T> propertyType) {
        Config config = ConfigProvider.getConfig();
        for (String propertyName : propertyNames) {
            Optional<T> value = config.getOptionalValue(propertyName, propertyType);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private static class ConfigBuilderComparator implements Comparator<ConfigBuilder> {
        private static final ConfigBuilderComparator INSTANCE = new ConfigBuilderComparator();

        @Override
        public int compare(ConfigBuilder o1, ConfigBuilder o2) {
            return Integer.compare(o1.priority(), o2.priority());
        }
    }
}
