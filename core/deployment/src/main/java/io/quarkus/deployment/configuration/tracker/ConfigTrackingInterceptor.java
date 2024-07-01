package io.quarkus.deployment.configuration.tracker;

import static io.smallrye.config.SecretKeys.doLocked;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.Config;

import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;

/**
 * Build configuration interceptor that records all the configuration options
 * and their values that are read during the build.
 */
@Priority(Priorities.APPLICATION)
public class ConfigTrackingInterceptor implements ConfigSourceInterceptor {

    /**
     * A writer that persists collected configuration options and their values to a file
     */
    public interface ConfigurationWriter {
        void write(ConfigTrackingConfig config, BuildTimeConfigurationReader.ReadResult configReadResult,
                LaunchMode launchMode, Path buildDirectory);
    }

    /**
     * Provides an immutable map of options that were read during the build.
     */
    public interface ReadOptionsProvider {

        /**
         * An immutable map of options read during the build.
         *
         * @return immutable map of options read during the build
         */
        Map<String, String> getReadOptions();
    }

    private boolean enabled;
    // it's a String value map to be able to represent null (not configured) values
    private Map<String, String> readOptions = Map.of();
    private final ReadOptionsProvider readOptionsProvider = new ReadOptionsProvider() {
        @Override
        public Map<String, String> getReadOptions() {
            return Collections.unmodifiableMap(readOptions);
        }
    };

    /**
     * Initializes the configuration tracker
     *
     * @param config configuration instance
     */
    public void configure(Config config) {
        enabled = config.getOptionalValue("quarkus.config-tracking.enabled", boolean.class).orElse(false);
        if (enabled) {
            readOptions = new ConcurrentHashMap<>();
        }
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        if (!enabled) {
            return context.proceed(name);
        }
        final ConfigValue configValue = doLocked(() -> context.proceed(name));
        readOptions.put(name, ConfigTrackingValueTransformer.asString(configValue));
        return configValue;
    }

    /**
     * Read options orvipder.
     *
     * @return read options provider
     */
    public ReadOptionsProvider getReadOptionsProvider() {
        return readOptionsProvider;
    }
}
