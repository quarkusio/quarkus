package io.quarkus.runtime.configuration;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Utilities for Configuration.
 */
public final class ConfigUtils {
    private ConfigUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a {@link SmallRyeConfigBuilder} with the expected default configuration to build a
     * {@link io.smallrye.config.Config} instance for the current {@link LaunchMode}.
     * <p>
     * See {@link ConfigUtils#configBuilder(LaunchMode)} for the builder defaults.
     *
     * @return a {@link SmallRyeConfigBuilder}
     * @see ConfigUtils#configBuilder() (LaunchMode)
     * @see ConfigUtils#emptyConfigBuilder(LaunchMode)
     */
    public static SmallRyeConfigBuilder configBuilder() {
        return configBuilder(LaunchMode.current());
    }

    /**
     * Returns a {@link SmallRyeConfigBuilder} with the expected default configuration to build a
     * {@link io.smallrye.config.Config} instance for the specified {@link LaunchMode}.
     * <p>
     * By default, this builder discovers {@link io.smallrye.config.SmallRyeConfigBuilderCustomizer} and
     * {@link org.eclipse.microprofile.config.spi.ConfigSource} registered with the ServiceLoader mechanism.
     * <p>
     * See {@link ConfigUtils#emptyConfigBuilder(LaunchMode)} for additional builder defaults.
     *
     * @param mode the {@link LaunchMode}
     * @return a {@link SmallRyeConfigBuilder}
     * @see ConfigUtils#emptyConfigBuilder(LaunchMode)
     */
    public static SmallRyeConfigBuilder configBuilder(LaunchMode mode) {
        return emptyConfigBuilder(mode).addDiscoveredCustomizers().addDiscoveredSources();
    }

    /**
     * Returns a {@link SmallRyeConfigBuilder} with the expected default configuration to build a
     * {@link io.smallrye.config.Config} instance for the current {@link LaunchMode}.
     * <p>
     * See {@link ConfigUtils#emptyConfigBuilder(LaunchMode)} for the builder defaults.
     *
     * @return a {@link SmallRyeConfigBuilder}
     * @see ConfigUtils#emptyConfigBuilder(LaunchMode)
     */
    public static SmallRyeConfigBuilder emptyConfigBuilder() {
        return emptyConfigBuilder(LaunchMode.current());
    }

    /**
     * Returns a {@link SmallRyeConfigBuilder} with the expected default configuration to build a
     * {@link io.smallrye.config.Config} instance for the specified {@link LaunchMode}.
     * <p>
     * By default, this builder discovers {@link org.eclipse.microprofile.config.spi.Converter},
     * {@link io.smallrye.config.ConfigSourceInterceptor} and {@link io.smallrye.config.SecretKeysHandler} registered
     * with the ServiceLoader mechanism. It also adds the defaults {@link io.smallrye.config.ConfigSourceInterceptor}
     * and {@link org.eclipse.microprofile.config.spi.ConfigSource}, namely, interceptors for profiles and expressions,
     * and sources for {@code application.properties}, System Properties and Environment Variables.
     *
     * @param mode the {@link LaunchMode}
     * @return a {@link SmallRyeConfigBuilder}
     */
    public static SmallRyeConfigBuilder emptyConfigBuilder(LaunchMode mode) {
        return new SmallRyeConfigBuilder()
                .forClassLoader(Thread.currentThread().getContextClassLoader())
                .withCustomizers(new QuarkusConfigBuilderCustomizer(mode))
                .addDiscoveredConverters()
                .addDefaultInterceptors()
                .addDiscoveredInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .addDefaultSources();
    }

    /**
     * Returns a {@code List} of the active profiles in Quarkus.
     * <p>
     * Profiles are sorted in reverse order according to how they were set in
     * {@code quarkus.profile}, as the last profile overrides the previous one until there are
     * no profiles left in the list.
     *
     * @return a {@code List} of the active profiles
     * @see io.smallrye.config.SmallRyeConfig#getProfiles()
     */
    public static List<String> getProfiles() {
        return ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getProfiles();
    }

    /**
     * Check if a configuration profile is active in Quarkus.
     *
     * @param profile the configuration profile to check
     * @return true if the profile is active or false otherwise.
     */
    public static boolean isProfileActive(final String profile) {
        return getProfiles().contains(profile);
    }

    /**
     * Checks if a property is present in the current Configuration.
     * <p>
     * Because the sources may not expose the property directly in {@link ConfigSource#getPropertyNames()}, we cannot
     * reliably determine if the property is present in the properties list. The property needs to be retrieved to make
     * sure it exists. Also, if the value is an expression, we want to ignore expansion, because this is not relevant
     * for the check and the expansion value may not be available at this point.
     * <p>
     * It may be interesting to expose such API in SmallRyeConfig directly.
     *
     * @param propertyName the property name.
     * @return true if the property is present or false otherwise.
     */
    public static boolean isPropertyPresent(String propertyName) {
        return ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).isPropertyPresent(propertyName);
    }

    /**
     * Checks if a property has non-empty value in the current Configuration.
     * <p>
     * This method is similar to {@link #isPropertyPresent(String)}, but does not ignore expression expansion.
     *
     * @param propertyName the property name.
     * @return true if the property is present or false otherwise.
     */
    public static boolean isPropertyNonEmpty(String propertyName) {
        ConfigValue configValue = ConfigProvider.getConfig().getConfigValue(propertyName);
        return configValue.getValue() != null && !configValue.getValue().isEmpty();
    }

    /**
     * Checks if any of the given properties is present in the current Configuration.
     * <p>
     * Because the sources may not expose the property directly in {@link ConfigSource#getPropertyNames()}, we cannot
     * reliably determine if the property is present in the properties list. The property needs to be retrieved to make
     * sure it exists. Also, if the value is an expression, we want to ignore expansion, because this is not relevant
     * for the check and the expansion value may not be available at this point.
     * <p>
     * It may be interesting to expose such API in SmallRyeConfig directly.
     *
     * @param propertyNames The configuration property names
     * @return true if the property is present or false otherwise.
     */
    public static boolean isAnyPropertyPresent(Collection<String> propertyNames) {
        for (String propertyName : propertyNames) {
            if (isPropertyPresent(propertyName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the value of the first given property present in the current Configuration,
     * or {@link Optional#empty()} if none of the properties is present.
     *
     * @param <T> The property type
     * @param propertyNames The configuration property names
     * @param propertyType The type that the resolved property value should be converted to
     * @return the first resolved property value as a {@code Optional} instance of the property type
     */
    public static <T> Optional<T> getFirstOptionalValue(List<String> propertyNames, Class<T> propertyType) {
        Config config = ConfigProvider.getConfig();
        for (String propertyName : propertyNames) {
            Optional<T> value = config.getOptionalValue(propertyName, propertyType);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }
}
