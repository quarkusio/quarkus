package io.quarkus.gradle.tasks;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;

/**
 * A Gradle {@link ValueSource} that builds the Quarkus configuration map in isolation from
 * Gradle's configuration cache input tracking.
 * <p>
 * Gradle instruments {@code System.getProperties()} to track all system property accesses as
 * configuration cache inputs. SmallRyeConfig internally accesses system properties when building
 * its config sources and iterating property names. By wrapping this work in a {@code ValueSource},
 * the individual system property accesses are invisible to the configuration cache — Gradle only
 * tracks the final result map as a single opaque input.
 * <p>
 * The result is re-evaluated on every build. If the result changes (i.e., the effective
 * configuration has changed), the configuration cache entry is invalidated. This provides
 * correct invalidation behavior without false positives from unrelated system property changes.
 * <p>
 * The returned map is filtered to only include quarkus-relevant properties and a small set of
 * stable JVM properties needed for expression expansion. Volatile system properties (like
 * {@code java.vm.version}) are excluded to prevent spurious cache invalidation.
 */
public abstract class QuarkusConfigValueSource
        implements ValueSource<Map<String, String>, QuarkusConfigValueSource.Params> {

    /**
     * JVM system properties included in the output for expression expansion in
     * config mapping defaults (e.g., {@code quarkus.native.java-home} defaults to {@code ${java.home}}).
     * These are stable across builds (tied to JVM installation, not build invocation).
     */
    private static final String[] EXPRESSION_EXPANSION_PROPERTIES = { "java.home", "user.home" };

    interface Params extends ValueSourceParameters {
        MapProperty<String, String> getBuildProperties();

        MapProperty<String, String> getProjectProperties();

        SetProperty<File> getSourceDirectories();

        Property<String> getProfile();
    }

    @Nullable
    @Override
    public Map<String, String> obtain() {
        Params params = getParameters();

        Set<File> sourceDirs = params.getSourceDirectories().getOrElse(Collections.emptySet());

        // Build EffectiveConfig with full system sources inside this ValueSource boundary.
        // All System.getProperties() access happens here and is invisible to CC input tracking.
        EffectiveConfig effectiveConfig = EffectiveConfig.builder()
                .withTaskProperties(Collections.emptyMap())
                .withBuildProperties(params.getBuildProperties().getOrElse(Collections.emptyMap()))
                .withProjectProperties(params.getProjectProperties().getOrElse(Collections.emptyMap()))
                .withSourceDirectories(sourceDirs)
                .withProfile(params.getProfile().getOrElse("prod"))
                .build();

        // Filter the full config map to only quarkus-relevant properties.
        // The full map includes ALL system properties (from SysPropConfigSource), which would
        // cause the ValueSource result to change when unrelated properties change — defeating
        // the purpose of using a ValueSource. We filter to keep the output stable.
        Map<String, String> fullMap = effectiveConfig.getValues();
        Map<String, String> filtered = new HashMap<>();

        for (Map.Entry<String, String> entry : fullMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("quarkus.")
                    || key.startsWith("platform.quarkus.")
                    || key.startsWith("smallrye.config.")) {
                filtered.put(key, entry.getValue());
            }
        }

        // Include stable JVM properties needed for expression expansion when BaseConfig
        // reconstructs a SmallRyeConfig from this map.
        for (String prop : EXPRESSION_EXPANSION_PROPERTIES) {
            String value = System.getProperty(prop);
            if (value != null) {
                filtered.putIfAbsent(prop, value);
            }
        }

        return Collections.unmodifiableMap(filtered);
    }
}
