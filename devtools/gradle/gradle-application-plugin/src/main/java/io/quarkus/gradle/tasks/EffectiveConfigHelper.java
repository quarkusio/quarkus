package io.quarkus.gradle.tasks;

import static java.util.Collections.unmodifiableMap;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.gradle.api.logging.Logger;

import io.quarkus.gradle.QuarkusPlugin;
import io.smallrye.config.AbstractLocationConfigSourceLoader;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.source.yaml.YamlConfigSource;

/**
 * Helper that bundles the various sources of config options for the Gradle plugin: system environment, system properties,
 * quarkus build properties (on the Quarkus extension), Gradle project properties, application properties and "forced"
 * properties (on the Gradle task).
 *
 * <p>
 * Eventually used to construct a map with the <em>effective</em> config options from all the sources above.
 */
final class EffectiveConfigHelper {
    // System properties starting with 'quarkus.'
    final Map<String, String> quarkusSystemProperties;
    // Environment properties starting with 'QUARKUS_'
    final Map<String, String> quarkusEnvProperties;
    // Gradle project + extension properties starting with 'quarkus.'
    private Map<String, String> quarkusBuildProperties;
    private Map<String, String> projectProperties;
    private Map<String, String> applicationProperties;
    private Map<String, String> forcedProperties;
    final List<URL> applicationPropertiesSourceUrls = new ArrayList<>();

    EffectiveConfigHelper() {
        this(collectQuarkusSystemProperties(), collectQuarkusEnvProperties());
    }

    EffectiveConfigHelper(Map<String, String> quarkusSystemProperties, Map<String, String> quarkusEnvProperties) {
        this.quarkusSystemProperties = quarkusSystemProperties;
        this.quarkusEnvProperties = quarkusEnvProperties;
    }

    EffectiveConfigHelper applyBuildProperties(Map<String, String> buildProperties) {
        quarkusBuildProperties = unmodifiableMap(collectProperties(buildProperties));
        return this;
    }

    EffectiveConfigHelper applyProjectProperties(Map<String, ?> buildProperties) {
        projectProperties = collectProperties(buildProperties);
        return this;
    }

    EffectiveConfigHelper applyApplicationProperties(Set<File> sourceDirectories, Logger logger) {
        applicationProperties = loadApplicationProperties(sourceDirectories, logger, applicationPropertiesSourceUrls::add);
        return this;
    }

    EffectiveConfigHelper applyForcedProperties(Map<String, String> forcedProperties) {
        this.forcedProperties = forcedProperties;
        return this;
    }

    Map<String, String> buildEffectiveConfiguration() {
        Map<String, String> map = new HashMap<>();

        // Add non-"profile-prefixed" configuration options (aka all that do not start with '%')
        addNonProfileToEffectiveConfig(projectProperties, map);
        addNonProfileToEffectiveConfig(quarkusBuildProperties, map);
        addNonProfileToEffectiveConfig(applicationProperties, map);
        addNonProfileToEffectiveConfig(quarkusEnvProperties, map);
        addNonProfileToEffectiveConfig(quarkusSystemProperties, map);
        addNonProfileToEffectiveConfig(forcedProperties, map);

        String quarkusProfile = map.getOrDefault(QuarkusPlugin.QUARKUS_PROFILE, QuarkusPlugin.DEFAULT_PROFILE);
        String profilePrefix = "%" + quarkusProfile + ".";

        // Add the configuration options for the selected profile (filtering on the key + truncating the key)
        addProfileToEffectiveConfig(projectProperties, profilePrefix, map);
        addProfileToEffectiveConfig(quarkusBuildProperties, profilePrefix, map);
        addProfileToEffectiveConfig(applicationProperties, profilePrefix, map);
        addProfileToEffectiveConfig(quarkusEnvProperties, profilePrefix, map);
        addProfileToEffectiveConfig(quarkusSystemProperties, profilePrefix, map);
        addProfileToEffectiveConfig(forcedProperties, profilePrefix, map);

        return unmodifiableMap(map);
    }

    static Map<String, String> loadApplicationProperties(Set<File> sourceDirectories, Logger logger, Consumer<URL> sourceUrls) {
        URL[] resourceUrls = sourceDirectories.stream().map(File::toURI)
                .map(u -> {
                    try {
                        return u.toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(URL[]::new);

        Map<String, String> config = new HashMap<>();

        if (logger.isInfoEnabled()) {
            logger.info("Loading Quarkus application config from resource URLs {}",
                    Arrays.stream(resourceUrls).map(Object::toString).collect(Collectors.joining(", ")));
        }

        CombinedConfigSourceProvider configSourceProvider = new CombinedConfigSourceProvider(sourceUrls);
        logger.debug("Loading Quarkus application config");
        for (URL resourceUrl : resourceUrls) {
            URLClassLoader classLoader = new URLClassLoader(new URL[] { resourceUrl });
            for (ConfigSource configSource : configSourceProvider.getConfigSources(classLoader)) {
                Map<String, String> properties = configSource.getProperties();
                logger.debug("Loaded {} Quarkus application config entries via {}", properties.size(),
                        configSource);
                config.putAll(properties);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Loaded Quarkus application config from 'application.[properties|yaml|yml]: {}",
                    new TreeMap<>(config).entrySet().stream().map(Objects::toString)
                            .collect(Collectors.joining("\n    ", "\n    ", "")));
        }

        return unmodifiableMap(config);
    }

    static final class CombinedConfigSourceProvider extends AbstractLocationConfigSourceLoader implements ConfigSourceProvider {
        final Consumer<URL> sourceUrls;

        CombinedConfigSourceProvider(Consumer<URL> sourceUrls) {
            this.sourceUrls = sourceUrls;
        }

        @Override
        protected String[] getFileExtensions() {
            return new String[] {
                    "application.yaml",
                    "application.yml",
                    "application.properties"
            };
        }

        @Override
        protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
            sourceUrls.accept(url);
            return url.getPath().endsWith(".properties") ? new PropertiesConfigSource(url, ordinal)
                    : new YamlConfigSource(url, ordinal);
        }

        @Override
        public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
            // Note:
            return loadConfigSources(new String[] { "application.yml", "application.yaml", "application.properties" }, 260,
                    classLoader);
        }
    }

    static Map<String, String> collectQuarkusSystemProperties() {
        return collectQuarkusSystemProperties(System.getProperties());
    }

    static Map<String, String> collectQuarkusSystemProperties(Map<Object, Object> systemProperties) {
        return unmodifiableMap(collectProperties(systemProperties));
    }

    static Map<String, String> collectQuarkusEnvProperties() {
        return collectQuarkusEnvProperties(System.getenv());
    }

    static Map<String, String> collectQuarkusEnvProperties(Map<String, String> env) {
        Map<String, String> quarkusEnvProperties = new HashMap<>();
        env.forEach((k, v) -> {
            if (k.startsWith("QUARKUS_")) {
                // convert environment name to property key
                String key = k.toLowerCase(Locale.ROOT).replace('_', '.');
                quarkusEnvProperties.put(key, v);
            }
        });
        return unmodifiableMap(quarkusEnvProperties);
    }

    static Map<String, String> collectProperties(Map<?, ?> source) {
        Map<String, String> target = new HashMap<>();
        source.forEach((k, v) -> {
            String key = k.toString();
            if (key.startsWith("quarkus.") && v instanceof String) {
                target.put(key, (String) v);
            }
        });
        return target;
    }

    static void addNonProfileToEffectiveConfig(Map<String, ?> source, Map<String, String> map) {
        source.forEach((k, v) -> {
            if (v instanceof String && !k.startsWith("%")) {
                map.put(k, (String) v);
            }
        });
    }

    static void addProfileToEffectiveConfig(Map<String, ?> source, String profilePrefix, Map<String, String> map) {
        source.forEach((k, v) -> {
            if (v instanceof String && k.startsWith(profilePrefix)) {
                map.put(k.substring(profilePrefix.length()), (String) v);
            }
        });
    }
}
