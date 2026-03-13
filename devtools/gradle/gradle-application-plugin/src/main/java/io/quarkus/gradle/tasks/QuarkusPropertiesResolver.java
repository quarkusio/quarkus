package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.EffectiveConfig.toUrlClassloader;
import static io.quarkus.gradle.tasks.QuarkusGradleUtils.getSourceSet;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

import io.quarkus.gradle.QuarkusPlugin;
import io.quarkus.gradle.extension.QuarkusPluginExtension;
import io.smallrye.config.PropertiesConfigSourceLoader;

/**
 * Properties resolver used when
 * {@link io.quarkus.gradle.extension.QuarkusPluginExtension#getDisableCreatingBuildConfigDuringConfiguration()}
 * is enabled.
 * It resolves properties from system properties and the {@code application.properties} file in the resources directory,
 * following the same order as https://quarkus.io/guides/config-reference#configuration-sources.
 */
public class QuarkusPropertiesResolver {
    private static final String QUARKUS_PREFIX = "quarkus.";
    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final int APPLICATION_PROPERTIES_ORDINAL = 250;
    private static final String DEFAULT_JAR_TYPE = "fast-jar";
    private static final String DEFAULT_RUNNER_SUFFIX = "-runner";
    protected static final String KEY_NATIVE_ENABLED = "quarkus.native.enabled";
    protected static final String KEY_JAR_ENABLED = "quarkus.package.jar.enabled";
    protected static final String KEY_OUTPUT_DIRECTORY = "quarkus.package.output-directory";
    protected static final String KEY_OUTPUT_NAME = "quarkus.package.output-name";
    protected static final String KEY_ADD_RUNNER_SUFFIX = "quarkus.package.jar.add-runner-suffix";
    protected static final String KEY_NATIVE_SOURCES_ONLY = "quarkus.native.sources-only";
    protected static final String KEY_JAR_TYPE = "quarkus.package.jar.type";
    protected static final String KEY_RUNNER_SUFFIX = "quarkus.package.jar.runner-suffix";
    protected static final String KEY_IGNORED_ENTRIES = "quarkus.package.ignored-entries";

    private QuarkusPropertiesResolver() {
    }

    public static Map<String, String> resolve(Project project, QuarkusPluginExtension extension) {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put(KEY_NATIVE_ENABLED, "false");
        defaults.put(KEY_JAR_ENABLED, "true");
        defaults.put(KEY_OUTPUT_DIRECTORY, QuarkusPlugin.DEFAULT_OUTPUT_DIRECTORY);
        defaults.put(KEY_OUTPUT_NAME, extension.finalName());
        defaults.put(KEY_ADD_RUNNER_SUFFIX, "true");
        defaults.put(KEY_NATIVE_SOURCES_ONLY, "false");
        defaults.put(KEY_JAR_TYPE, DEFAULT_JAR_TYPE);
        defaults.put(KEY_RUNNER_SUFFIX, DEFAULT_RUNNER_SUFFIX);
        defaults.put(KEY_IGNORED_ENTRIES, "");

        Map<String, String> properties = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            properties.put(entry.getKey(), systemPropertyOrDefault(project, entry.getKey(), entry.getValue()));
        }
        addApplicationProperties(project, properties);

        return properties;
    }

    private static String systemPropertyOrDefault(Project project, String key, String defaultValue) {
        Provider<String> provider = project.getProviders().systemProperty(key);
        return provider.getOrElse(defaultValue);
    }

    private static void addApplicationProperties(Project project, Map<String, String> properties) {
        Set<File> resourceDirs = getSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME)
                .getResources()
                .getSourceDirectories()
                .getFiles();
        List<ConfigSource> sources = PropertiesConfigSourceLoader.inClassPath(
                APPLICATION_PROPERTIES,
                APPLICATION_PROPERTIES_ORDINAL,
                toUrlClassloader(resourceDirs));
        for (ConfigSource configSource : sources) {
            for (String name : configSource.getPropertyNames()) {
                if (name.startsWith(QUARKUS_PREFIX)) {
                    properties.putIfAbsent(name, configSource.getValue(name));
                }
            }
        }
    }
}
