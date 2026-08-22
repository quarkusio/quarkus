package io.quarkus.gradle.application.internal.config;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.deployment.configuration.ConfigCompatibility;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.DefaultValuesConfigSource;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.Expressions;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SysPropConfigSource;
import io.smallrye.config.source.yaml.YamlConfigSourceLoader;

/**
 * Resolves the effective Quarkus configuration for named application builds while preserving the legacy Gradle plugin
 * source ordering and worker-propagation behavior.
 */
final class EffectiveConfig {
    private final Map<String, String> values;
    private final List<EffectiveConfigDiagnostic> diagnostics;
    private final Map<String, String> quarkusValues;
    private final int externallyProvidedValuesOmitted;
    private final List<String> configSourceNames;

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
                    Map.of("platform.quarkus.native.builder-image", "<<ignored>>"), "fallbackPlatformProperties", 0);
        } else {
            platformPropertiesConfigSource = new PropertiesConfigSource(builder.platformProperties, "platformProperties", 0);
        }

        Set<String> externallyProvidedSourceNames = new HashSet<>();
        SmallRyeConfigBuilder configBuilder = ConfigUtils.emptyConfigBuilder()
                .forClassLoader(toUrlClassloader(builder.sourceDirectories))
                .withSources(new PropertiesConfigSource(builder.forcedProperties, "forcedProperties", 600))
                .withSources(new PropertiesConfigSource(asStringMap(builder.taskProperties), "taskProperties", 500));
        if (builder.systemProperties == null && builder.environmentProperties == null) {
            configBuilder.addSystemSources();
        } else {
            if (builder.systemProperties != null) {
                PropertiesConfigSource systemProperties = new PropertiesConfigSource(
                        builder.systemProperties, SysPropConfigSource.NAME, 400);
                externallyProvidedSourceNames.add(systemProperties.getName());
                configBuilder.withSources(systemProperties);
            }
            if (builder.environmentProperties != null) {
                PropertiesConfigSource environmentProperties = new PropertiesConfigSource(
                        builder.environmentProperties, EnvConfigSource.NAME, 300);
                externallyProvidedSourceNames.add(environmentProperties.getName());
                configBuilder.withSources(environmentProperties);
            }
        }
        SmallRyeConfig resolvedConfig = configBuilder
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
        resolvedConfig.getConfigSources().forEach(source -> {
            if (source instanceof SysPropConfigSource || source instanceof EnvConfigSource) {
                externallyProvidedSourceNames.add(source.getName());
            }
        });
        this.values = generateFullConfigMap(resolvedConfig);
        DiagnosticConfig diagnosticConfig = generateDiagnosticConfigMap(resolvedConfig, externallyProvidedSourceNames);
        this.diagnostics = diagnosticConfig.entries();
        this.externallyProvidedValuesOmitted = diagnosticConfig.externallyProvidedValuesOmitted();
        this.quarkusValues = generateQuarkusConfigMap(resolvedConfig);
        Set<String> sourceNames = new LinkedHashSet<>();
        resolvedConfig.getConfigSources()
                .forEach(source -> sourceNames.add(diagnosticSourceName(source.getName(), source.getOrdinal())));
        this.configSourceNames = List.copyOf(sourceNames);
    }

    Map<String, String> getValues() {
        return values;
    }

    Map<String, String> getQuarkusValues() {
        return quarkusValues;
    }

    List<EffectiveConfigDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    int externallyProvidedValuesOmitted() {
        return externallyProvidedValuesOmitted;
    }

    List<String> getConfigSourceNames() {
        return configSourceNames;
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

    static Map<String, String> generateFullConfigMap(SmallRyeConfig config) {
        Set<String> excludeNames = excludedDefaultNames();
        return Expressions.withoutExpansion(new Supplier<Map<String, String>>() {
            @Override
            public Map<String, String> get() {
                Map<String, String> properties = new HashMap<>();
                for (String propertyName : config.getPropertyNames()) {
                    ConfigValue configValue = config.getConfigValue(propertyName);
                    if (configValue.getValue() != null) {
                        // Exclude defaults coming from PackageConfig and NativeConfig, as this Map as passed as
                        // system properties to Gradle workers and, we loose the ability to determine if it was set by
                        // the user to evaluate deprecated configuration
                        if (!excludeNames.contains(configValue.getName()) || !configValue.isDefault()) {
                            properties.put(propertyName, configValue.getValue());
                        }
                    }
                }
                return unmodifiableMap(properties);
            }
        });
    }

    private static DiagnosticConfig generateDiagnosticConfigMap(SmallRyeConfig config,
            Set<String> externallyProvidedSourceNames) {
        Set<String> excludeNames = excludedDefaultNames();
        return Expressions.withoutExpansion(new Supplier<DiagnosticConfig>() {
            @Override
            public DiagnosticConfig get() {
                List<EffectiveConfigDiagnostic> entries = new ArrayList<>();
                int externallyProvidedValuesOmitted = 0;
                for (String propertyName : config.getPropertyNames()) {
                    ConfigValue configValue = config.getConfigValue(propertyName);
                    if (configValue.getValue() == null) {
                        continue;
                    }
                    if (externallyProvidedSourceNames.contains(configValue.getConfigSourceName())) {
                        externallyProvidedValuesOmitted++;
                        continue;
                    }
                    if (!excludeNames.contains(configValue.getName()) || !configValue.isDefault()) {
                        entries.add(new EffectiveConfigDiagnostic(
                                propertyName,
                                configValue.getValue(),
                                diagnosticSourceName(configValue.getConfigSourceName(),
                                        configValue.getConfigSourceOrdinal()),
                                configValue.getConfigSourceOrdinal(),
                                configValue.isDefault()));
                    }
                }
                return new DiagnosticConfig(List.copyOf(entries), externallyProvidedValuesOmitted);
            }
        });
    }

    private static String diagnosticSourceName(String sourceName, int ordinal) {
        if (sourceName == null || sourceName.isBlank()) {
            return "configuration source (ordinal " + ordinal + ")";
        }
        if (sourceName.contains("forcedProperties")) {
            return "build operation";
        }
        if (sourceName.contains("taskProperties")) {
            return "task configuration";
        }
        if (sourceName.contains("quarkusBuildProperties")) {
            return "Quarkus build DSL";
        }
        if (sourceName.contains("projectProperties")) {
            return "Gradle project properties";
        }
        if (sourceName.contains("platformProperties") || sourceName.contains("fallbackPlatformProperties")) {
            return "platform properties";
        }
        if (DefaultValuesConfigSource.NAME.equals(sourceName)) {
            return "default values";
        }
        if (sourceName.contains(SysPropConfigSource.NAME)) {
            return "system properties";
        }
        if (sourceName.contains(EnvConfigSource.NAME)) {
            return "environment variables";
        }
        String applicationSource = applicationSourceName(sourceName);
        if (applicationSource != null) {
            return applicationSource;
        }
        return "configuration source (ordinal " + ordinal + ")";
    }

    private static String applicationSourceName(String sourceName) {
        String normalized = sourceName.replace('\\', '/').toLowerCase(Locale.ROOT);
        for (String fileName : List.of(
                "application.properties",
                "application.yaml",
                "application.yml",
                "microprofile-config.properties",
                "microprofile-config.yaml",
                "microprofile-config.yml")) {
            if (normalized.contains(fileName)) {
                return fileName;
            }
        }
        return null;
    }

    /**
     * Constructs a Map with the list of Quarkus property names that must be propagated to the Gradle workers.
     * <p>
     * This only takes into account the configuration that is not already available in Quarkus, including properties
     * provided by Gradle tasks or files. System Properties are also included, since Gradle workers run on a separate
     * VM that does not have access to the original System Properties set by the process.
     * <p>
     * Environment Variables do not require propagation, since all workers can the Environment. Configuration files
     * like {@code application.properties} are already available in Quarkus, so we can skip them as well.
     */
    static Map<String, String> generateQuarkusConfigMap(SmallRyeConfig config) {
        Set<String> excludeNames = excludedDefaultNames();
        Set<String> propagateSources = new HashSet<>();
        propagateSources.add("PropertiesConfigSource[source=forcedProperties]");
        propagateSources.add("PropertiesConfigSource[source=taskProperties]");
        propagateSources.add("PropertiesConfigSource[source=quarkusBuildProperties]");
        propagateSources.add("PropertiesConfigSource[source=projectProperties]");
        propagateSources.add("PropertiesConfigSource[source=platformProperties]");
        propagateSources.add(SysPropConfigSource.NAME);
        // It may look weird to include default values, but build requests use this map as explicit worker input.
        propagateSources.add(DefaultValuesConfigSource.NAME);
        return Expressions.withoutExpansion(new Supplier<Map<String, String>>() {
            @Override
            public Map<String, String> get() {
                Map<String, String> properties = new HashMap<>();
                for (String propertyName : config.getPropertyNames()) {
                    if (propertyName.startsWith("quarkus.") || propertyName.startsWith("platform.quarkus.")) {
                        ConfigValue configValue = config.getConfigValue(propertyName);

                        // Propagate properties from any source from the test namespace, because they can be read in the launcher
                        if (configValue.getValue() != null && configValue.getName().startsWith("quarkus.test.")) {
                            properties.put(propertyName, configValue.getValue());
                            continue;
                        }

                        // Only propagate properties from sources not available in Quarkus
                        if (configValue.getValue() != null && propagateSources.contains(configValue.getConfigSourceName())) {
                            // Exclude defaults coming from PackageConfig and NativeConfig, as this Map as passed as
                            // system properties to Gradle workers and, we loose the ability to determine if it was set by
                            // the user to evaluate deprecated configuration
                            if (!excludeNames.contains(configValue.getName()) || !configValue.isDefault()) {
                                properties.put(propertyName, configValue.getValue());
                            }
                        }
                    }
                }
                return unmodifiableMap(properties);
            }
        });
    }

    private static Set<String> excludedDefaultNames() {
        Set<String> excludeNames = new HashSet<>();
        excludeNames.addAll(configClass(PackageConfig.class).getProperties().keySet());
        excludeNames.addAll(configClass(NativeConfig.class).getProperties().keySet());
        return excludeNames;
    }

    static Builder builder() {
        return new Builder();
    }

    private record DiagnosticConfig(List<EffectiveConfigDiagnostic> entries, int externallyProvidedValuesOmitted) {
    }

    static final class Builder {
        private Map<String, String> platformProperties = emptyMap();
        private Map<String, String> forcedProperties = emptyMap();
        private Map<String, ?> taskProperties = emptyMap();
        private Map<String, String> buildProperties = emptyMap();
        private Map<String, ?> projectProperties = emptyMap();
        private Map<String, String> defaultProperties = emptyMap();
        private Map<String, String> systemProperties;
        private Map<String, String> environmentProperties;
        private Set<File> sourceDirectories = emptySet();
        private String profile = "prod";

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

        Builder withSystemProperties(Map<String, String> systemProperties) {
            this.systemProperties = systemProperties;
            return this;
        }

        Builder withEnvironmentProperties(Map<String, String> environmentProperties) {
            this.environmentProperties = environmentProperties;
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
