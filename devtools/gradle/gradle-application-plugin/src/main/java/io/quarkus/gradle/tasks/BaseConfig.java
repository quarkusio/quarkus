package io.quarkus.gradle.tasks;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.deployment.configuration.ConfigCompatibility;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.runtime.configuration.QuarkusConfigBuilderCustomizer;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

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

    /**
     * Constructs a BaseConfig from a pre-resolved configuration map, typically produced by
     * {@link QuarkusConfigValueSource}.
     * <p>
     * A lightweight {@link SmallRyeConfig} is built from the map to provide typed access to
     * {@link PackageConfig} and {@link NativeConfig} mappings. This SmallRyeConfig does NOT
     * use {@code addSystemSources()} or {@code addDefaultSources()} — all values come from
     * the pre-computed map, avoiding any {@code System.getProperties()} access that would
     * interfere with Gradle's configuration cache.
     */
    BaseConfig(Map<String, String> configValues) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withCustomizers(new QuarkusConfigBuilderCustomizer())
                .addDiscoveredConverters()
                .addDefaultInterceptors()
                .addDiscoveredInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withSources(new PropertiesConfigSource(configValues, "valueSourceConfig", 400))
                .withMapping(PackageConfig.class)
                .withMapping(NativeConfig.class)
                .withInterceptors(ConfigCompatibility.FrontEnd.nonLoggingInstance(), ConfigCompatibility.BackEnd.instance())
                .build();

        manifest = new Manifest();
        packageConfig = config.getConfigMapping(PackageConfig.class);
        nativeConfig = config.getConfigMapping(NativeConfig.class);

        // populate the Gradle Manifest object
        PackageConfig.JarConfig.ManifestConfig manifestConfig = packageConfig.jar().manifest();
        manifest.attributes(manifestConfig.attributes());
        manifestConfig.sections().forEach((section, attribs) -> manifest.attributes(attribs, section));

        values = configValues;
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
        TreeMap<String, String> result = new TreeMap<>();

        Predicate<Map.Entry<String, ?>> keyPredicate = e -> patterns.stream().anyMatch(p -> p.matcher(e.getKey()).matches());
        values.entrySet().stream()
                .filter(keyPredicate)
                .forEach(e -> result.put(e.getKey(), e.getValue()));

        // For caching-relevant properties not found in the values map, check environment variables.
        // This ensures that user-declared caching properties (like env vars) are still tracked as
        // task inputs and cause rebuilds when their values change.
        // Note: System.getenv(name) is a single env var lookup, which is CC-compatible — Gradle
        // only tracks that specific env var, not all env vars.
        for (String name : propertyPatterns) {
            if (!result.containsKey(name)) {
                String envValue = System.getenv(name);
                if (envValue != null) {
                    result.put(name, envValue);
                }
            }
        }

        return result;
    }
}
