package io.quarkus.runtime.configuration;

import static io.smallrye.config.PropertiesConfigSourceProvider.classPathSources;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;
import static io.smallrye.config.SmallRyeConfigBuilder.META_INF_MICROPROFILE_CONFIG_PROPERTIES;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.KeyMap;
import io.smallrye.config.NameIterator;
import io.smallrye.config.Priorities;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SysPropConfigSource;
import io.smallrye.config.common.MapBackedConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;

/**
 *
 */
public final class ConfigUtils {

    /**
     * The name of the property associated with a random UUID generated at launch time.
     */
    static final String UUID_KEY = "quarkus.uuid";
    public static final String QUARKUS_BUILD_TIME_RUNTIME_PROPERTIES = "quarkus-build-time-runtime.properties";
    public static final String QUARKUS_RUNTIME_CONFIG_DEFAULTS_PROPERTIES = "quarkus-runtime-config-defaults.properties";

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
        return configBuilder(runTime, false, addDiscovered, launchMode);
    }

    /**
     * Get the basic configuration builder.
     *
     * @param runTime {@code true} if the configuration is run time, {@code false} if build time
     * @param addDiscovered {@code true} if the ConfigSource and Converter objects should be auto-discovered
     * @return the configuration builder
     */
    public static SmallRyeConfigBuilder configBuilder(final boolean runTime, final boolean bootstrap,
            final boolean addDiscovered, final LaunchMode launchMode) {
        SmallRyeConfigBuilder builder = emptyConfigBuilder();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        builder.forClassLoader(classLoader);
        builder.withSources(new ApplicationPropertiesConfigSourceLoader.InFileSystem());
        builder.withSources(new ApplicationPropertiesConfigSourceLoader.InClassPath());
        if (launchMode.isDevOrTest() && (runTime || bootstrap)) {
            builder.withSources(new RuntimeOverrideConfigSource(classLoader));
        }
        if (runTime || bootstrap) {
            builder.addDefaultSources();
            // Validator only for runtime. We cannot use the current validator for build time (chicken / egg problem)
            builder.addDiscoveredValidator();
            builder.withDefaultValue(UUID_KEY, UUID.randomUUID().toString());
            builder.withSources(new DotEnvConfigSourceProvider());
            builder.withSources(new DefaultsConfigSource(loadBuildTimeRunTimeValues(), "BuildTime RunTime Fixed", MAX_VALUE));
            builder.withSources(new DefaultsConfigSource(loadRunTimeDefaultValues(), "RunTime Defaults", MIN_VALUE + 100));
        } else {
            List<ConfigSource> sources = new ArrayList<>();
            sources.addAll(classPathSources(META_INF_MICROPROFILE_CONFIG_PROPERTIES, classLoader));
            sources.addAll(new BuildTimeDotEnvConfigSourceProvider().getConfigSources(classLoader));
            sources.add(new BuildTimeEnvConfigSource());
            sources.add(new BuildTimeSysPropConfigSource());
            builder.withSources(sources);
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

        builder.addDefaultInterceptors();
        builder.addDiscoveredInterceptors();
        builder.addDiscoveredConverters();
        return builder;
    }

    @SuppressWarnings("unchecked")
    public static SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder, List<ConfigBuilder> configBuilders) {
        configBuilders.sort(ConfigBuilderComparator.INSTANCE);

        for (ConfigBuilder configBuilder : configBuilders) {
            builder = configBuilder.configBuilder(builder);
            if (builder == null) {
                throw new ConfigurationException(configBuilder.getClass().getName() + " returned a null builder");
            }
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

    public static Map<String, String> loadBuildTimeRunTimeValues() {
        try {
            URL resource = Thread.currentThread().getContextClassLoader().getResource(QUARKUS_BUILD_TIME_RUNTIME_PROPERTIES);
            return resource != null ? ConfigSourceUtil.urlToMap(resource) : Collections.emptyMap();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> loadRunTimeDefaultValues() {
        try {
            URL resource = Thread.currentThread().getContextClassLoader()
                    .getResource(QUARKUS_RUNTIME_CONFIG_DEFAULTS_PROPERTIES);
            return resource != null ? ConfigSourceUtil.urlToMap(resource) : Collections.emptyMap();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addMapping(SmallRyeConfigBuilder builder, String mappingClass, String prefix) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            builder.withMapping(contextClassLoader.loadClass(mappingClass), prefix);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * We override the EnvConfigSource, because we don't want the nothing back from getPropertiesNames at build time.
     * The mapping is one way and there is no way to map them back.
     */
    static class BuildTimeEnvConfigSource extends EnvConfigSource {
        BuildTimeEnvConfigSource() {
            super();
        }

        BuildTimeEnvConfigSource(final Map<String, String> propertyMap, final int ordinal) {
            super(propertyMap, ordinal);
        }

        @Override
        public Set<String> getPropertyNames() {
            return Collections.emptySet();
        }

        @Override
        public String getName() {
            return "System environment";
        }
    }

    /**
     * Same as BuildTimeEnvConfigSource.
     */
    static class BuildTimeDotEnvConfigSourceProvider extends DotEnvConfigSourceProvider {
        public BuildTimeDotEnvConfigSourceProvider() {
            super();
        }

        public BuildTimeDotEnvConfigSourceProvider(final String location) {
            super(location);
        }

        @Override
        protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
            return new BuildTimeEnvConfigSource(ConfigSourceUtil.urlToMap(url), ordinal) {
                @Override
                public String getName() {
                    return super.getName() + "[source=" + url + "]";
                }
            };
        }
    }

    /**
     * We only want to include properties in the quarkus namespace.
     *
     * We removed the filter on the quarkus namespace due to the any prefix support for ConfigRoot. Filtering is now
     * done in io.quarkus.deployment.configuration.BuildTimeConfigurationReader.ReadOperation#getAllProperties.
     */
    static class BuildTimeSysPropConfigSource extends SysPropConfigSource {
        public String getName() {
            return "System properties";
        }

        @Override
        public Set<String> getPropertyNames() {
            return Collections.emptySet();
        }
    }

    static class DefaultsConfigSource extends MapBackedConfigSource {
        private final KeyMap<String> wildcards;

        public DefaultsConfigSource(final Map<String, String> properties, final String name, final int ordinal) {
            // Defaults may contain wildcards, but we don't want to expose them in getPropertyNames, so we need to filter them
            super(name, filterWildcards(properties), ordinal);
            this.wildcards = new KeyMap<>();
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (entry.getKey().contains("*")) {
                    this.wildcards.findOrAdd(entry.getKey()).putRootValue(entry.getValue());
                }
            }
        }

        @Override
        public String getValue(final String propertyName) {
            String value = super.getValue(propertyName);
            return value == null ? wildcards.findRootValue(propertyName) : value;
        }

        private static Map<String, String> filterWildcards(final Map<String, String> properties) {
            Map<String, String> filtered = new HashMap<>();
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (entry.getKey().contains("*")) {
                    continue;
                }
                filtered.put(entry.getKey(), entry.getValue());
            }
            return filtered;
        }
    }

    private static class ConfigBuilderComparator implements Comparator<ConfigBuilder> {

        private static final ConfigBuilderComparator INSTANCE = new ConfigBuilderComparator();

        private ConfigBuilderComparator() {
        }

        @Override
        public int compare(ConfigBuilder o1, ConfigBuilder o2) {
            return Integer.compare(o1.priority(), o2.priority());
        }
    }
}
