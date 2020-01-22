package io.quarkus.runtime.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.PropertiesConfigSourceProvider;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 *
 */
public final class ConfigUtils {
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

    /**
     * Get the basic configuration builder.
     *
     * @param runTime {@code true} if the configuration is run time, {@code false} if build time
     * @return the configuration builder
     */
    public static SmallRyeConfigBuilder configBuilder(final boolean runTime) {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        final ApplicationPropertiesConfigSource.InFileSystem inFileSystem = new ApplicationPropertiesConfigSource.InFileSystem();
        final ApplicationPropertiesConfigSource.InJar inJar = new ApplicationPropertiesConfigSource.InJar();
        final ApplicationPropertiesConfigSource.MpConfigInJar mpConfig = new ApplicationPropertiesConfigSource.MpConfigInJar();
        builder.withSources(inFileSystem, inJar, mpConfig);
        final ExpandingConfigSource.Cache cache = new ExpandingConfigSource.Cache();
        builder.withWrapper(ExpandingConfigSource.wrapper(cache));
        builder.withWrapper(DeploymentProfileConfigSource.wrapper());
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (runTime) {
            builder.addDefaultSources();
        } else {
            final List<ConfigSource> sources = new ArrayList<>();
            sources.addAll(new PropertiesConfigSourceProvider("META-INF/microprofile-config.properties", true, classLoader)
                    .getConfigSources(classLoader));
            // required by spec...
            sources.addAll(
                    new PropertiesConfigSourceProvider("WEB-INF/classes/META-INF/microprofile-config.properties", true,
                            classLoader).getConfigSources(classLoader));
            sources.add(new EnvConfigSource());
            sources.add(new SysPropConfigSource());
            builder.withSources(sources.toArray(new ConfigSource[0]));
        }
        builder.addDiscoveredSources();
        builder.addDiscoveredConverters();
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

    static final class EnvConfigSource implements ConfigSource {
        static final Pattern REP_PATTERN = Pattern.compile("[^a-zA-Z0-9_]");

        public Map<String, String> getProperties() {
            return Collections.emptyMap();
        }

        public String getValue(final String propertyName) {
            return System.getenv(REP_PATTERN.matcher(propertyName.toUpperCase(Locale.ROOT)).replaceAll("_"));
        }

        public String getName() {
            return "System environment";
        }
    }

    static final class SysPropConfigSource implements ConfigSource {
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

        public String getValue(final String propertyName) {
            return System.getProperty(propertyName);
        }

        public String getName() {
            return "System properties";
        }
    }
}
