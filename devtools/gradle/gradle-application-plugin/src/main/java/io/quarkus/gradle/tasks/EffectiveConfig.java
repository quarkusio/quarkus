package io.quarkus.gradle.tasks;

import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.annotations.VisibleForTesting;

import io.quarkus.deployment.configuration.ClassLoadingConfig;
import io.quarkus.deployment.configuration.ConfigCompatibility;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.QuarkusConfigBuilderCustomizer;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Expressions;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSourceLoader;

/**
 * Helper that bundles the various sources of config options for the Gradle plugin: system environment, system properties,
 * quarkus build properties (on the Quarkus extension), Gradle project properties, application properties and "forced/task"
 * properties (on the Gradle task).
 *
 * <p>
 * Eventually used to construct a map with the <em>effective</em> config options from all the sources above and expose
 * the Quarkus config objects like {@link PackageConfig}, {@link ClassLoadingConfig} and the underlying {@link SmallRyeConfig}.
 */
public final class EffectiveConfig {
    private final SmallRyeConfig config;
    private volatile Map<String, String> values;
    private volatile Map<String, String> quarkusValues;
    private final Map<String, String> cachingValues;

    private EffectiveConfig(Builder builder) {
        // Effective "ordinals" for the config sources:
        // (see also https://quarkus.io/guides/config-reference#configuration-sources)
        // 600 -> forcedProperties
        // 500 -> taskProperties
        // 400 -> System.getProperties() (provided by default sources)
        // 300 -> System.getenv() (provided by default sources)
        // 290 -> quarkusBuildProperties
        // 280 -> projectProperties
        // 265 -> application.(yaml/yml) in config folder
        // 260 -> application.properties in config folder (provided by default sources)
        // 255 -> application.(yaml|yml) in classpath
        // 250 -> application.properties in classpath (provided by default sources)
        // 110 -> microprofile.(yaml|yml) in classpath
        // 100 -> microprofile.properties in classpath (provided by default sources)
        // 0 -> fallback config source for error workaround (see below)

        PropertiesConfigSource platformPropertiesConfigSource;
        if (builder.platformProperties.isEmpty()) {
            // we don't have the model yet so we don't have the Platform properties around
            platformPropertiesConfigSource = new PropertiesConfigSource(
                    Map.of("platform.quarkus.native.builder-image", "<<ignored>>"), "platformProperties", 0);
        } else {
            platformPropertiesConfigSource = new PropertiesConfigSource(builder.platformProperties, "platformProperties", 0);
        }

        SmallRyeConfigBuilder configBuilder;
        if (builder.useSystemSources) {
            // Full builder with all default sources (including System.getProperties() and System.getenv()).
            // Used during task execution where reading all system properties is fine.
            configBuilder = ConfigUtils.emptyConfigBuilder()
                    .addSystemSources();
        } else {
            // Minimal builder WITHOUT addDefaultSources() to avoid reading System.getProperties()/System.getenv(),
            // which would cause Gradle's configuration cache to track all system properties as inputs.
            // Used during configuration phase with only quarkus-prefixed properties passed explicitly.
            configBuilder = new SmallRyeConfigBuilder()
                    .withCustomizers(new QuarkusConfigBuilderCustomizer())
                    .addDiscoveredConverters()
                    .addDefaultInterceptors()
                    .addDiscoveredInterceptors()
                    .addDiscoveredSecretKeysHandlers();
            if (builder.filteredSystemProperties != null) {
                configBuilder.withSources(
                        new PropertiesConfigSource(builder.filteredSystemProperties, "filteredSystemProperties", 400));
            }
        }
        configBuilder
                .forClassLoader(toUrlClassloader(builder.sourceDirectories))
                .withSources(new PropertiesConfigSource(builder.forcedProperties, "forcedProperties", 600))
                .withSources(new PropertiesConfigSource(asStringMap(builder.taskProperties), "taskProperties", 500));
        this.config = configBuilder
                .withSources(new PropertiesConfigSource(builder.buildProperties, "quarkusBuildProperties", 290))
                .withSources(new PropertiesConfigSource(asStringMap(builder.projectProperties), "projectProperties", 280))
                .withSources(new YamlConfigSourceLoader.InFileSystem())
                .withSources(new YamlConfigSourceLoader.InClassPath())
                .addPropertiesSources()
                .withSources(platformPropertiesConfigSource)
                .withDefaultValues(builder.defaultProperties)
                .withProfile(builder.profile)
                .withMapping(PackageConfig.class)
                .withMapping(NativeConfig.class)
                .withInterceptors(ConfigCompatibility.FrontEnd.instance(), ConfigCompatibility.BackEnd.instance())
                .build();
        this.cachingValues = generateCachingConfigMap(config);
    }

    public SmallRyeConfig getConfig() {
        return config;
    }

    /**
     * Returns the full configuration map including all property names from all sources.
     * <p>
     * This method is lazily computed because it calls {@link SmallRyeConfig#getPropertyNames()} which reads
     * all system properties. It should only be called during task execution, not during Gradle's configuration phase,
     * to avoid unnecessary configuration cache invalidation.
     */
    public Map<String, String> getValues() {
        if (values == null) {
            values = generateFullConfigMap(config);
        }
        return values;
    }

    /**
     * Returns a configuration map containing only quarkus.* and platform.quarkus.* properties.
     * <p>
     * This method is lazily computed because it calls {@link SmallRyeConfig#getPropertyNames()} which reads
     * all system properties. It should only be called during task execution, not during Gradle's configuration phase,
     * to avoid unnecessary configuration cache invalidation.
     */
    public Map<String, String> getQuarkusValues() {
        if (quarkusValues == null) {
            quarkusValues = generateQuarkusConfigMap(config);
        }
        return quarkusValues;
    }

    /**
     * Returns a configuration map containing only properties from {@link PackageConfig} and {@link NativeConfig}.
     * <p>
     * Unlike {@link #getValues()}, this method does not iterate all property names from all config sources,
     * which avoids reading all system properties. This makes it safe to call during Gradle's configuration phase
     * without causing unnecessary configuration cache invalidation when unrelated system properties change.
     */
    public Map<String, String> getCachingValues() {
        return cachingValues;
    }

    private Map<String, String> asStringMap(Map<String, ?> map) {
        Map<String, String> target = new HashMap<>();
        map.forEach((k, v) -> {
            if (v != null) {
                target.put(k, v.toString());
            }
        });
        return target;
    }

    @VisibleForTesting
    static Map<String, String> generateFullConfigMap(SmallRyeConfig config) {
        Set<String> defaultNames = new HashSet<>();
        defaultNames.addAll(configClass(PackageConfig.class).getProperties().keySet());
        defaultNames.addAll(configClass(NativeConfig.class).getProperties().keySet());
        return Expressions.withoutExpansion(new Supplier<Map<String, String>>() {
            @Override
            public Map<String, String> get() {
                Map<String, String> properties = new HashMap<>();
                for (String propertyName : config.getPropertyNames()) {
                    ConfigValue configValue = config.getConfigValue(propertyName);
                    // Remove defaults coming from PackageConfig and NativeConfig, as this Map as passed as
                    // system properties to Gradle workers and, we loose the ability to determine if it was set by
                    // the user to evaluate deprecated configuration
                    if (configValue.getValue() != null && (!defaultNames.contains(configValue.getName())
                            || !configValue.isDefault())) {
                        properties.put(propertyName, configValue.getValue());
                    }
                }
                return unmodifiableMap(properties);
            }
        });
    }

    static Map<String, String> generateQuarkusConfigMap(SmallRyeConfig config) {
        Set<String> defaultNames = new HashSet<>();
        defaultNames.addAll(configClass(PackageConfig.class).getProperties().keySet());
        defaultNames.addAll(configClass(NativeConfig.class).getProperties().keySet());
        return Expressions.withoutExpansion(new Supplier<Map<String, String>>() {
            @Override
            public Map<String, String> get() {
                Map<String, String> properties = new HashMap<>();
                for (String propertyName : config.getPropertyNames()) {
                    if (propertyName.startsWith("quarkus.") || propertyName.startsWith("platform.quarkus.")) {
                        ConfigValue configValue = config.getConfigValue(propertyName);
                        // Remove defaults coming from PackageConfig and NativeConfig, as this Map as passed as
                        // system properties to Gradle workers and, we loose the ability to determine if it was set by
                        // the user to evaluate deprecated configuration
                        if (configValue.getValue() != null && (!defaultNames.contains(configValue.getName())
                                || !configValue.isDefault())) {
                            properties.put(propertyName, configValue.getValue());
                        }
                    }
                }
                return unmodifiableMap(properties);
            }
        });
    }

    /**
     * Generates a configuration map containing only properties defined by {@link PackageConfig} and {@link NativeConfig}
     * mapping classes. This avoids calling {@link SmallRyeConfig#getPropertyNames()} which would read all system
     * properties and cause Gradle configuration cache invalidation when unrelated system properties change.
     * <p>
     * Instead, only the known property names from the mapping metadata are looked up individually in the config,
     * so only those specific properties are tracked by Gradle's configuration cache.
     */
    @VisibleForTesting
    static Map<String, String> generateCachingConfigMap(SmallRyeConfig config) {
        Set<String> mappingPropertyNames = new HashSet<>();
        mappingPropertyNames.addAll(configClass(PackageConfig.class).getProperties().keySet());
        mappingPropertyNames.addAll(configClass(NativeConfig.class).getProperties().keySet());
        return Expressions.withoutExpansion(new Supplier<Map<String, String>>() {
            @Override
            public Map<String, String> get() {
                Map<String, String> properties = new HashMap<>();
                for (String propertyName : mappingPropertyNames) {
                    ConfigValue configValue = config.getConfigValue(propertyName);
                    if (configValue.getValue() != null && !configValue.isDefault()) {
                        properties.put(propertyName, configValue.getValue());
                    }
                }
                return unmodifiableMap(properties);
            }
        });
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private Map<String, String> platformProperties = emptyMap();
        private Map<String, String> forcedProperties = emptyMap();
        private Map<String, ?> taskProperties = emptyMap();
        private Map<String, String> buildProperties = emptyMap();
        private Map<String, ?> projectProperties = emptyMap();
        private Map<String, String> defaultProperties = emptyMap();
        private Set<File> sourceDirectories = emptySet();
        private String profile = "prod";
        private boolean useSystemSources = true;
        private Map<String, String> filteredSystemProperties = null;

        EffectiveConfig build() {
            return new EffectiveConfig(this);
        }

        Builder withPlatformProperties(Map<String, String> platformProperties) {
            this.platformProperties = platformProperties;
            return this;
        }

        Builder withForcedProperties(Map<String, String> forcedProperties) {
            this.forcedProperties = forcedProperties;
            return this;
        }

        Builder withTaskProperties(Map<String, ?> taskProperties) {
            this.taskProperties = taskProperties;
            return this;
        }

        Builder withBuildProperties(Map<String, String> buildProperties) {
            this.buildProperties = buildProperties;
            return this;
        }

        Builder withProjectProperties(Map<String, ?> projectProperties) {
            this.projectProperties = projectProperties;
            return this;
        }

        Builder withDefaultProperties(Map<String, String> defaultProperties) {
            this.defaultProperties = defaultProperties;
            return this;
        }

        Builder withSourceDirectories(Set<File> sourceDirectories) {
            this.sourceDirectories = sourceDirectories;
            return this;
        }

        Builder withProfile(String profile) {
            this.profile = profile;
            return this;
        }

        /**
         * Replaces {@code addSystemSources()} (which reads ALL system properties and env vars) with
         * a pre-filtered map of system properties. This avoids Gradle's configuration cache tracking
         * all system properties as inputs, which would cause cache invalidation when any unrelated
         * system property changes (e.g. IDE-injected properties).
         * <p>
         * The filtered properties are added at ordinal 400, matching the ordinal of
         * {@code System.getProperties()} in the default config source ordering.
         */
        Builder withFilteredSystemProperties(Map<String, String> systemProperties) {
            this.useSystemSources = false;
            this.filteredSystemProperties = systemProperties;
            return this;
        }
    }

    /**
     * Builds a specific {@link ClassLoader} for {@link SmallRyeConfig} to include potential configuration files in
     * the application source paths. The {@link ClassLoader} excludes the path <code>META-INF/services</code> because
     * in most cases, the ServiceLoader files will reference service implementations that are not yet compiled. It is
     * possible that the service files reference implementations from dependencies, which are valid and, in this case,
     * wrongly excluded, but most likely only required for the application and not the Gradle build. We will rewrite
     * the implementation to cover that case if this becomes an issue.
     *
     * @param sourceDirectories a Set of source directories specified by the Gradle build.
     * @return a {@link ClassLoader} with the source paths
     */
    private static ClassLoader toUrlClassloader(Set<File> sourceDirectories) {
        List<URL> urls = new ArrayList<>();
        for (File sourceDirectory : sourceDirectories) {
            try {
                urls.add(sourceDirectory.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        return new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader()) {
            @Override
            public URL getResource(String name) {
                if (name.startsWith("META-INF/services/")) {
                    return null;
                }
                return super.getResource(name);
            }

            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                if (name.startsWith("META-INF/services/")) {
                    return Collections.emptyEnumeration();
                }
                return super.getResources(name);
            }
        };
    }
}
