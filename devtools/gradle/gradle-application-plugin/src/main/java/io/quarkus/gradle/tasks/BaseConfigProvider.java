package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.QuarkusPropertiesResolver.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.java.archives.Attributes;

import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.QuarkusPlugin;
import io.quarkus.gradle.extension.QuarkusPluginExtension;

/**
 * BaseConfigProvider is used to provide configuration values to the tasks during the configuration phase.
 * By default it provides the BaseConfig values from the plugin extension, but when
 * {@link io.quarkus.gradle.extension.QuarkusPluginExtension#getDisableCreatingBuildConfigDuringConfiguration()}
 * is enabled, it resolves the properties from system properties and the
 * {@code application.properties} file in the resources directory.
 */
public class BaseConfigProvider {

    private final Boolean disableCreatingBuildConfigDuringConfiguration;
    private final QuarkusPluginExtension extension;

    private final Map<String, String> properties;

    private final Project project;

    public BaseConfigProvider(Boolean disableCreatingBuildConfigDuringConfiguration,
            Project project,
            QuarkusPluginExtension extension) {
        this.disableCreatingBuildConfigDuringConfiguration = disableCreatingBuildConfigDuringConfiguration;
        this.extension = extension;
        if (disableCreatingBuildConfigDuringConfiguration) {
            properties = QuarkusPropertiesResolver.resolve(project, extension);
        } else {
            properties = Map.of();
        }
        this.project = project;
    }

    public Boolean getJarEnabled() {
        if (disableCreatingBuildConfigDuringConfiguration) {
            return Boolean.parseBoolean(properties.get(KEY_JAR_ENABLED));
        } else {
            return extension.packageConfig().jar().enabled();
        }
    }

    public Boolean getNativeEnabled() {
        if (disableCreatingBuildConfigDuringConfiguration) {
            return Boolean.parseBoolean(properties.get(KEY_NATIVE_ENABLED));
        } else {
            return extension.nativeConfig().enabled();
        }
    }

    public Path getOutputDirectory() {
        if (disableCreatingBuildConfigDuringConfiguration) {
            return Path.of(properties.get(KEY_OUTPUT_DIRECTORY));
        } else {
            return Path.of(extension.packageConfig().outputDirectory().map(Path::toString)
                    .orElse(QuarkusPlugin.DEFAULT_OUTPUT_DIRECTORY));
        }
    }

    public String getOutputName() {
        if (disableCreatingBuildConfigDuringConfiguration) {
            return properties.get(KEY_OUTPUT_NAME);
        } else {
            return extension.packageConfig().outputName().orElseGet(extension::finalName);
        }
    }

    public Boolean getNativeSourcesOnly() {
        if (disableCreatingBuildConfigDuringConfiguration) {
            return Boolean.parseBoolean(properties.get(KEY_NATIVE_SOURCES_ONLY));
        } else {
            return extension.nativeConfig().sourcesOnly();
        }
    }

    public PackageConfig.JarConfig.JarType getJarType() {
        if (disableCreatingBuildConfigDuringConfiguration) {
            return PackageConfig.JarConfig.JarType.fromString(properties.get(KEY_JAR_TYPE));
        } else {
            return extension.packageConfig().jar().type();
        }
    }

    public String getRunnerSuffix() {
        if (disableCreatingBuildConfigDuringConfiguration) {
            if (Boolean.parseBoolean(properties.get(KEY_ADD_RUNNER_SUFFIX))) {
                return properties.get(KEY_RUNNER_SUFFIX);
            } else {
                return "";
            }
        } else {
            return extension.packageConfig().computedRunnerSuffix();
        }
    }

    public Map<String, String> getCachingRelevantProperties(List<String> cachingRelevantProperties) {
        if (disableCreatingBuildConfigDuringConfiguration) {
            return cachingRelevantProperties.stream()
                    .filter(s -> !s.matches("quarkus\\..*") && !s.matches("platform\\.quarkus\\..*"))
                    .map(s -> Map.entry(s, project.getProviders().environmentVariable(s)))
                    .filter(e -> e.getValue().isPresent())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().get()));

        } else {
            return extension.cachingRelevantProperties(extension.getCachingRelevantProperties().get());
        }

    }

    public Map<String, Object> getManifestAttributes() {
        if (disableCreatingBuildConfigDuringConfiguration) {
            return Map.of();
        } else {
            return extension.manifest().getAttributes();
        }
    }

    public Map<String, Attributes> getManifestSections() {
        if (disableCreatingBuildConfigDuringConfiguration) {
            return Map.of();
        } else {
            return extension.manifest().getSections();
        }
    }

    public List<String> getIgnoredEntries() {
        if (disableCreatingBuildConfigDuringConfiguration) {
            String userIgnoredEntries = properties.get(KEY_IGNORED_ENTRIES);
            if (userIgnoredEntries != null && !userIgnoredEntries.isEmpty()) {
                return List.of(userIgnoredEntries.split(","));
            } else {
                return List.of();
            }
        } else {
            return extension.ignoredEntriesProperty().get();
        }
    }
}
