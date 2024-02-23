package io.quarkus.gradle.tasks;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.runtime.configuration.ConfigInstantiator;

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
    private final Map<String, String> values;

    // Note: EffectiveConfig has all the code to load the configurations from all the sources.
    BaseConfig(EffectiveConfig config) {
        manifest = new Manifest();
        packageConfig = new PackageConfig();

        ConfigInstantiator.handleObject(packageConfig, config.getConfig());

        // populate the Gradle Manifest object
        manifest.attributes(packageConfig.manifest.attributes);
        packageConfig.manifest.manifestSections.forEach((section, attribs) -> manifest.attributes(attribs, section));

        values = config.getValues();
    }

    PackageConfig packageConfig() {
        return packageConfig;
    }

    PackageConfig.BuiltInType packageType() {
        return PackageConfig.BuiltInType.fromString(packageConfig.type);
    }

    Manifest manifest() {
        return manifest;
    }

    Map<String, String> cachingRelevantProperties(List<String> propertyPatterns) {
        List<Pattern> patterns = propertyPatterns.stream().map(s -> "^(" + s + ")$").map(Pattern::compile)
                .collect(Collectors.toList());
        Predicate<Map.Entry<String, ?>> keyPredicate = e -> patterns.stream().anyMatch(p -> p.matcher(e.getKey()).matches());
        return values.entrySet().stream()
                .filter(keyPredicate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
