package io.quarkus.gradle.tasks;

import static java.util.Collections.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import com.google.common.annotations.VisibleForTesting;

import io.quarkus.deployment.configuration.ClassLoadingConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.AbstractLocationConfigSourceLoader;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.PropertiesConfigSource;
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
final class EffectiveConfig {
    private final Map<String, String> fullConfig;

    // URLs of all application.properties/yaml/yml files that were consulted (including those that do not exist)
    private final List<URL> applicationPropsSources;
    private final SmallRyeConfig config;

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
        // 250 -> application.(properties|yaml|yml) (in classpath/source)
        // 100 -> microprofile.properties (in classpath/source)

        applicationPropsSources = new ArrayList<>();

        configSources.add(new PropertiesConfigSource(builder.forcedProperties, "forcedProperties", 600));
        configSources.add(new PropertiesConfigSource(asStringMap(builder.taskProperties), "taskProperties", 500));
        configSources.add(new PropertiesConfigSource(ConfigSourceUtil.propertiesToMap(System.getProperties()),
                "System.getProperties()", 400));
        configSources.add(new EnvConfigSource(300) {
        });
        configSources.add(new PropertiesConfigSource(builder.buildProperties, "quarkusBuildProperties", 290));
        configSources.add(new PropertiesConfigSource(asStringMap(builder.projectProperties), "projectProperties", 280));

        configSourcesForApplicationProperties(builder.sourceDirectories, applicationPropsSources::add, configSources::add, 250,
                new String[] {
                        "application.properties",
                        "application.yaml",
                        "application.yml"
                });
        configSourcesForApplicationProperties(builder.sourceDirectories, applicationPropsSources::add, configSources::add, 100,
                new String[] {
                        "microprofile-config.properties"
                });
        this.config = buildConfig(builder.profile, configSources);

        this.fullConfig = generateFullConfigMap(config);
    }

    SmallRyeConfig config() {
        return config;
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
        Map<String, String> map = new HashMap<>();
        config.getPropertyNames().forEach(property -> {
            String v = config.getConfigValue(property).getValue();
            if (v != null) {
                map.put(property, v);
            }
        });
        return unmodifiableMap(map);
    }

    @VisibleForTesting
    static SmallRyeConfig buildConfig(String profile, List<ConfigSource> configSources) {
        return ConfigUtils.emptyConfigBuilder()
                .setAddDiscoveredSecretKeysHandlers(false)
                .withSources(configSources)
                .withProfile(profile)
                .build();
    }

    static Builder builder() {
        return new Builder();
    }

    Map<String, String> configMap() {
        return fullConfig;
    }

    List<URL> applicationPropsSources() {
        return applicationPropsSources;
    }

    static void configSourcesForApplicationProperties(Set<File> sourceDirectories, Consumer<URL> sourceUrls,
            Consumer<ConfigSource> configSourceConsumer, int ordinal, String[] fileExtensions) {
        URL[] resourceUrls = sourceDirectories.stream().map(File::toURI)
                .map(u -> {
                    try {
                        return u.toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(URL[]::new);

        for (URL resourceUrl : resourceUrls) {
            URLClassLoader classLoader = new URLClassLoader(new URL[] { resourceUrl });
            CombinedConfigSourceProvider configSourceProvider = new CombinedConfigSourceProvider(sourceUrls, ordinal,
                    fileExtensions);
            configSourceProvider.getConfigSources(classLoader).forEach(configSourceConsumer);
        }
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

    static final class CombinedConfigSourceProvider extends AbstractLocationConfigSourceLoader implements ConfigSourceProvider {
        private final Consumer<URL> sourceUrls;
        private final int ordinal;
        private final String[] fileExtensions;

        CombinedConfigSourceProvider(Consumer<URL> sourceUrls, int ordinal, String[] fileExtensions) {
            this.sourceUrls = sourceUrls;
            this.ordinal = ordinal;
            this.fileExtensions = fileExtensions;
        }

        @Override
        protected String[] getFileExtensions() {
            return fileExtensions;
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
            return loadConfigSources(getFileExtensions(), ordinal, classLoader);
        }
    }
}
