package io.quarkus.gradle.tasks;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.dsl.Manifest;

/**
 * Required parts of the configuration used to <em>configure</em> a Quarkus build task, does not contain settings
 * via the {@link io.quarkus.gradle.extension.QuarkusPluginExtension} or any "forced properties".
 *
 * <p>
 * Configuration from system properties, environment, application.properties/yaml/yml, project properties is
 * available in a Gradle task's configuration phase.
 */
final class BaseConfig {
    private final Manifest manifest;
    private final PackageConfig packageConfig;
    private final NativeConfig nativeConfig;
    private final Map<String, String> values;

    // Note: EffectiveConfig has all the code to load the configurations from all the sources.
    BaseConfig(EffectiveConfig config) {
        manifest = new Manifest();
        packageConfig = config.getConfig().getConfigMapping(PackageConfig.class);
        nativeConfig = config.getConfig().getConfigMapping(NativeConfig.class);

        // populate the Gradle Manifest object
        PackageConfig.JarConfig.ManifestConfig manifestConfig = packageConfig.jar().manifest();
        manifest.attributes(manifestConfig.attributes());
        manifestConfig.sections().forEach((section, attribs) -> manifest.attributes(attribs, section));

        values = config.getValues();
    }

    PackageConfig packageConfig() {
        return packageConfig;
    }

    NativeConfig nativeConfig() {
        return nativeConfig;
    }

    PackageConfig.JarConfig.JarType jarType() {
        return packageConfig().jar().type();
    }

    Manifest manifest() {
        return manifest;
    }

    Map<String, String> cachingRelevantProperties(List<String> propertyPatterns) {
        List<Pattern> patterns = propertyPatterns.stream().map(s -> "^(" + s + ")$").map(Pattern::compile)
                .collect(Collectors.toList());
        readMissingEnvVariables(propertyPatterns);
        Predicate<Map.Entry<String, ?>> keyPredicate = e -> patterns.stream().anyMatch(p -> p.matcher(e.getKey()).matches());
        return values.entrySet().stream()
                .filter(keyPredicate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (s, s2) -> {
                    throw new IllegalArgumentException("Duplicate key");
                }, TreeMap::new));
    }

    /**
     * Reads missing environment variables that have been defined as `cachingRelevantProperties`.
     * This ensures that the configuration cache tracks these variables as inputs and detects changes in them.
     */
    private void readMissingEnvVariables(List<String> cachingRelevantProperties) {
        cachingRelevantProperties.stream()
                .filter(name -> !values.containsKey(name))
                .forEach(name -> System.getenv(name));
    }
}
