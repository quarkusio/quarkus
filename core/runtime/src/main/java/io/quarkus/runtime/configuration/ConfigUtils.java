package io.quarkus.runtime.configuration;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.DefaultValuesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 *
 */
public final class ConfigUtils {
    private ConfigUtils() {
    }

    public static SmallRyeConfigBuilder configBuilder() {
        return emptyConfigBuilder().addDiscoveredCustomizers().addDiscoveredSources();
    }

    public static SmallRyeConfigBuilder emptyConfigBuilder() {
        return new SmallRyeConfigBuilder()
                .forClassLoader(Thread.currentThread().getContextClassLoader())
                .withCustomizers(new QuarkusConfigBuilderCustomizer())
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
        return getConfig().getProfiles();
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
     *
     * @param propertyName the property name.
     * @return true if the property is present or false otherwise
     */
    public static boolean isPropertyPresent(final String propertyName) {
        return getConfig().isPropertyPresent(propertyName);
    }

    /**
     * Checks if a property has non-empty value in the current Configuration.
     * <p>
     * This method is similar to {@link #isPropertyPresent(String)}, but does not ignore expression expansion.
     *
     * @param propertyName the property name
     * @return true if the property is present or false otherwise
     */
    public static boolean isPropertyNonEmpty(final String propertyName) {
        ConfigValue configValue = getConfig().getConfigValue(propertyName);
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
     * @return true if the property is present or false otherwise
     */
    public static boolean isAnyPropertyPresent(final Collection<String> propertyNames) {
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
     * @return the first resolved property value as a {@code Optional} instance of the property type or {@link Optional#empty()}
     */
    public static <T> Optional<T> getFirstOptionalValue(final Collection<String> propertyNames, final Class<T> propertyType) {
        SmallRyeConfig config = getConfig();
        for (String propertyName : propertyNames) {
            Optional<T> value = config.getOptionalValue(propertyName, propertyType);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if a property has a default value in the current Configuration.
     *
     * @param propertyName the property name
     * @return true if the property has a default or false otherwise
     */
    public static boolean isPropertyDefaultValue(final String propertyName) {
        ConfigValue configValue = getConfig().getConfigValue(propertyName);
        return configValue.getValue() != null && DefaultValuesConfigSource.NAME.equals(configValue.getSourceName());
    }

    /**
     * Get the {@linkplain io.smallrye.config.SmallRyeConfig configuration} corresponding to the current Quarkus
     * config phase, as defined by the calling thread's {@linkplain Thread#getContextClassLoader() context class loader}.
     * <p>
     * The {@link io.smallrye.config.SmallRyeConfig} instance will be created and registered to the context class
     * loader if no such configuration is already created and registered.
     * <p>
     * Each class loader corresponds to exactly one configuration.
     *
     * @return the configuration instance for the thread context class loader
     */
    public static SmallRyeConfig getConfig() {
        return ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
    }
}
