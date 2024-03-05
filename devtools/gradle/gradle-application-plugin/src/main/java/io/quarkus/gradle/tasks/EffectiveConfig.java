package io.quarkus.gradle.tasks;

import static io.smallrye.config.SmallRyeConfigBuilder.META_INF_MICROPROFILE_CONFIG_PROPERTIES;
import static java.util.Collections.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import com.google.common.annotations.VisibleForTesting;

import io.quarkus.deployment.configuration.ClassLoadingConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.runtime.configuration.ApplicationPropertiesConfigSourceLoader;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.AbstractLocationConfigSourceLoader;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.Expressions;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.PropertiesConfigSourceProvider;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.common.utils.ConfigSourceUtil;
import io.smallrye.config.source.yaml.YamlConfigSource;

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
        List<ConfigSource> configSources = new ArrayList<>();
        // TODO add io.quarkus.runtime.configuration.DefaultsConfigSource ?
        // TODO leverage io.quarkus.runtime.configuration.ProfileManager ?

        // Effective "ordinals" for the config sources:
        // (see also https://quarkus.io/guides/config-reference#configuration-sources)
        // 600 -> forcedProperties
        // 500 -> taskProperties
        // 400 -> System.getProperties()
        // 300 -> System.getenv()
        // 290 -> quarkusBuildProperties
        // 280 -> projectProperties
        // 255 -> application,(yaml|yml) (in classpath/source)
        // 250 -> application.properties (in classpath/source)
        // 100 -> microprofile.properties (in classpath/source)

        configSources.add(new PropertiesConfigSource(builder.forcedProperties, "forcedProperties", 600));
        configSources.add(new PropertiesConfigSource(asStringMap(builder.taskProperties), "taskProperties", 500));
        configSources.add(new PropertiesConfigSource(ConfigSourceUtil.propertiesToMap(System.getProperties()),
                "System.getProperties()", 400));
        configSources.add(new EnvConfigSource(300) {
        });
        configSources.add(new PropertiesConfigSource(builder.buildProperties, "quarkusBuildProperties", 290));
        configSources.add(new PropertiesConfigSource(asStringMap(builder.projectProperties), "projectProperties", 280));

        ClassLoader classLoader = toUrlClassloader(builder.sourceDirectories);
        ApplicationPropertiesConfigSourceLoader.InClassPath applicationProperties = new ApplicationPropertiesConfigSourceLoader.InClassPath();
        configSources.addAll(applicationProperties.getConfigSources(classLoader));
        ApplicationYamlConfigSourceLoader.InClassPath applicationYaml = new ApplicationYamlConfigSourceLoader.InClassPath();
        configSources.addAll(applicationYaml.getConfigSources(classLoader));
        configSources
                .addAll(PropertiesConfigSourceProvider.classPathSources(META_INF_MICROPROFILE_CONFIG_PROPERTIES, classLoader));

        this.config = buildConfig(builder.profile, configSources);
        this.values = generateFullConfigMap(config);
    }

    public SmallRyeConfig getConfig() {
        return config;
    }

    public Map<String, String> getValues() {
        return values;
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

    @VisibleForTesting
    static SmallRyeConfig buildConfig(String profile, List<ConfigSource> configSources) {
        return ConfigUtils.emptyConfigBuilder()
                .setAddDiscoveredSecretKeysHandlers(false)
                // We add our own sources for environment, system-properties and microprofile-config.properties,
                // no need to include those twice.
                .setAddDefaultSources(false)
                .withSources(configSources)
                .withProfile(profile)
                .build();
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private Map<String, String> buildProperties = emptyMap();
        private Map<String, ?> projectProperties = emptyMap();
        private Map<String, ?> taskProperties = emptyMap();
        private Map<String, String> forcedProperties = emptyMap();
        private Set<File> sourceDirectories = emptySet();
        private String profile = "prod";

        EffectiveConfig build() {
            return new EffectiveConfig(this);
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

        Builder withSourceDirectories(Set<File> sourceDirectories) {
            this.sourceDirectories = sourceDirectories;
            return this;
        }

        Builder withProfile(String profile) {
            this.profile = profile;
            return this;
        }
    }

    private static ClassLoader toUrlClassloader(Set<File> sourceDirectories) {
        List<URL> urls = new ArrayList<>();
        for (File sourceDirectory : sourceDirectories) {
            try {
                urls.add(sourceDirectory.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]));
    }

    // Copied from quarkus-config-yaml. May be replaced by adding the quarkus-config-yaml dependency
    public static class ApplicationYamlConfigSourceLoader extends AbstractLocationConfigSourceLoader {
        @Override
        protected String[] getFileExtensions() {
            return new String[] {
                    "yaml",
                    "yml"
            };
        }

        @Override
        protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
            return new YamlConfigSource(url, ordinal);
        }

        public static class InClassPath extends ApplicationYamlConfigSourceLoader implements ConfigSourceProvider {
            @Override
            public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
                List<ConfigSource> configSources = new ArrayList<>();
                configSources.addAll(loadConfigSources("application.yaml", 255, classLoader));
                configSources.addAll(loadConfigSources("application.yml", 255, classLoader));
                return configSources;
            }

            @Override
            protected List<ConfigSource> tryFileSystem(final URI uri, final int ordinal) {
                return new ArrayList<>();
            }
        }
    }
}
