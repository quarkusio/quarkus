package io.quarkus.runtime.configuration;

import static io.smallrye.config.DotEnvConfigSourceProvider.dotEnvSources;
import static io.smallrye.config.PropertiesConfigSourceProvider.classPathSources;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;
import static io.smallrye.config.SmallRyeConfigBuilder.META_INF_MICROPROFILE_CONFIG_PROPERTIES;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.IntFunction;

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
import io.smallrye.config.Priorities;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SysPropConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;

/**
 *
 */
public final class ConfigUtils {

    /**
     * The name of the property associated with a random UUID generated at launch time.
     */
    static final String UUID_KEY = "quarkus.uuid";
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

    public static SmallRyeConfigBuilder configBuilder(final boolean runTime, LaunchMode launchMode) {
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
            LaunchMode launchMode) {
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
            final boolean addDiscovered,
            LaunchMode launchMode) {
        final SmallRyeConfigBuilder builder = emptyConfigBuilder();

        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        builder.withSources(new ApplicationPropertiesConfigSourceLoader.InFileSystem().getConfigSources(classLoader));
        builder.withSources(new ApplicationPropertiesConfigSourceLoader.InClassPath().getConfigSources(classLoader));
        if (launchMode.isDevOrTest() && (runTime || bootstrap)) {
            builder.withSources(new RuntimeOverrideConfigSource(classLoader));
        }
        if (runTime) {
            builder.addDefaultSources();
            builder.withDefaultValue(UUID_KEY, UUID.randomUUID().toString());
            builder.withSources(dotEnvSources(classLoader));
        } else {
            final List<ConfigSource> sources = new ArrayList<>();
            sources.addAll(classPathSources(META_INF_MICROPROFILE_CONFIG_PROPERTIES, classLoader));
            sources.addAll(new BuildTimeDotEnvConfigSourceProvider().getConfigSources(classLoader));
            sources.add(new BuildTimeEnvConfigSource());
            sources.add(new BuildTimeSysPropConfigSource());
            builder.withSources(sources);
        }
        if (addDiscovered) {
            builder.addDiscoveredSources();
        }
        if (runTime || bootstrap) {
            Map<String, String> runtimeDefaults = loadRuntimeDefaultValues();
            builder.withSources(new PropertiesConfigSource(runtimeDefaults, "Runtime Defaults", Integer.MIN_VALUE + 50));
        }
        return builder;
    }

    public static SmallRyeConfigBuilder emptyConfigBuilder() {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.withDefaultValue(SMALLRYE_CONFIG_PROFILE, ProfileManager.getActiveProfile());

        final Map<String, String> relocations = new HashMap<>();
        relocations.put(SMALLRYE_CONFIG_LOCATIONS, "quarkus.config.locations");
        relocations.put(SMALLRYE_CONFIG_PROFILE_PARENT, "quarkus.config.profile.parent");
        // Override the priority, because of the ProfileConfigSourceInterceptor and profile.parent.
        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new RelocateConfigSourceInterceptor(relocations);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 600 - 5);
            }
        });

        final Map<String, String> fallbacks = new HashMap<>();
        fallbacks.put("quarkus.config.locations", SMALLRYE_CONFIG_LOCATIONS);
        fallbacks.put("quarkus.config.profile.parent", SMALLRYE_CONFIG_PROFILE_PARENT);
        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new FallbackConfigSourceInterceptor(fallbacks);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 400 - 5);
            }
        });

        builder.addDefaultInterceptors();
        builder.addDiscoveredInterceptors();
        builder.addDiscoveredConverters();
        builder.addDiscoveredValidator();
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

    public static Map<String, String> loadRuntimeDefaultValues() {
        Map<String, String> values = new HashMap<>();
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(QUARKUS_RUNTIME_CONFIG_DEFAULTS_PROPERTIES)) {
            if (in == null) {
                return values;
            }
            Properties p = new Properties();
            p.load(in);
            for (String k : p.stringPropertyNames()) {
                if (!values.containsKey(k)) {
                    values.put(k, p.getProperty(k));
                }
            }
            return values;
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

    /**
     * Checks if a property is present in the current Configuration.
     * <p>
     * Because the sources may not expose the property directly in {@link ConfigSource#getPropertyNames()}, we cannot
     * reliable determine if the property is present in the properties list. The property needs to be retrieved to make
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
}
