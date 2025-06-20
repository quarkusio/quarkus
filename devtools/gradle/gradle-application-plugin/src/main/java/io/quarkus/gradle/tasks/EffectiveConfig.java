package io.quarkus.gradle.tasks;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;

import io.quarkus.deployment.configuration.ClassLoadingConfig;
import io.quarkus.deployment.configuration.ConfigCompatibility;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.Expressions;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
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
    private final Map<String, String> values;

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

        this.config = ConfigUtils.emptyConfigBuilder()
                .forClassLoader(toUrlClassloader(builder.sourceDirectories))
                .withSources(new PropertiesConfigSource(builder.forcedProperties, "forcedProperties", 600))
                .withSources(new PropertiesConfigSource(asStringMap(builder.taskProperties), "taskProperties", 500))
                .addSystemSources()
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
        this.values = generateFullConfigMap(config);
    }

    public SmallRyeConfig getConfig() {
        return config;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public Map<String, String> getOnlyQuarkusValues() {
        return values.entrySet().stream().filter(e -> e.getKey().startsWith("quarkus."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
        return Expressions.withoutExpansion(new Supplier<Map<String, String>>() {
            @Override
            public Map<String, String> get() {
                Map<String, String> properties = new HashMap<>();
                for (String propertyName : config.getPropertyNames()) {
                    String value = config.getRawValue(propertyName);
                    if (value != null) {
                        properties.put(propertyName, value);
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
