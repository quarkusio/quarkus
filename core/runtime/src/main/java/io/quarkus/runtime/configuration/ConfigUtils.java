package io.quarkus.runtime.configuration;

import static io.smallrye.config.AbstractLocationConfigSourceFactory.SMALLRYE_LOCATIONS;
import static io.smallrye.config.DotEnvConfigSourceProvider.dotEnvSources;
import static io.smallrye.config.ProfileConfigSourceInterceptor.SMALLRYE_PROFILE;
import static io.smallrye.config.ProfileConfigSourceInterceptor.SMALLRYE_PROFILE_PARENT;
import static io.smallrye.config.PropertiesConfigSourceProvider.classPathSources;
import static io.smallrye.config.SmallRyeConfigBuilder.META_INF_MICROPROFILE_CONFIG_PROPERTIES;
import static io.smallrye.config.SmallRyeConfigBuilder.WEB_INF_MICROPROFILE_CONFIG_PROPERTIES;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.IntFunction;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.DotEnvConfigSourceProvider;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.Priorities;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SysPropConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;

/**
 *
 */
public final class ConfigUtils {
    private static final Logger log = Logger.getLogger("io.quarkus.config");

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

    public static SmallRyeConfigBuilder configBuilder(final boolean runTime) {
        return configBuilder(runTime, true);
    }

    /**
     * Get the basic configuration builder.
     *
     * @param runTime {@code true} if the configuration is run time, {@code false} if build time
     * @param addDiscovered {@code true} if the ConfigSource and Converter objects should be auto-discovered
     * @return the configuration builder
     */
    public static SmallRyeConfigBuilder configBuilder(final boolean runTime, final boolean addDiscovered) {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.withDefaultValue(SMALLRYE_PROFILE, ProfileManager.getActiveProfile());

        final Map<String, String> relocations = new HashMap<>();
        relocations.put(SMALLRYE_LOCATIONS, "quarkus.config.locations");
        relocations.put(SMALLRYE_PROFILE_PARENT, "quarkus.config.profile.parent");
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

        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final ApplicationPropertiesConfigSource.InFileSystem inFileSystem = new ApplicationPropertiesConfigSource.InFileSystem();
        final ApplicationPropertiesConfigSource.InJar inJar = new ApplicationPropertiesConfigSource.InJar();
        builder.withSources(inFileSystem, inJar);
        builder.addDefaultInterceptors();
        if (runTime) {
            builder.addDefaultSources();
            builder.withSources(dotEnvSources(classLoader));
        } else {
            final List<ConfigSource> sources = new ArrayList<>();
            sources.addAll(classPathSources(META_INF_MICROPROFILE_CONFIG_PROPERTIES, classLoader));
            sources.addAll(classPathSources(WEB_INF_MICROPROFILE_CONFIG_PROPERTIES, classLoader));
            sources.addAll(new BuildTimeDotEnvConfigSourceProvider().getConfigSources(classLoader));
            sources.add(new BuildTimeEnvConfigSource());
            sources.add(new BuildTimeSysPropConfigSource());
            builder.withSources(sources);
        }
        if (addDiscovered) {
            builder.addDiscoveredSources();
            builder.addDiscoveredInterceptors();
            builder.addDiscoveredConverters();
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
            return new HashSet<>();
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
     */
    static class BuildTimeSysPropConfigSource extends SysPropConfigSource {
        public Map<String, String> getProperties() {
            Map<String, String> output = new TreeMap<>();
            for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
                String key = (String) entry.getKey();
                if (key.startsWith("quarkus.")) {
                    output.put(key, entry.getValue().toString());
                }
            }
            return output;
        }

        public String getName() {
            return "System properties";
        }
    }
}
