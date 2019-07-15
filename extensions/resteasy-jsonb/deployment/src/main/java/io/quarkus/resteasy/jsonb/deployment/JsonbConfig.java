package io.quarkus.resteasy.jsonb.deployment;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.json.bind.config.PropertyOrderStrategy;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = BUILD_TIME)
public class JsonbConfig {

    static final Set<String> ALLOWED_PROPERTY_ORDER_VALUES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(PropertyOrderStrategy.LEXICOGRAPHICAL, PropertyOrderStrategy.ANY,
                    PropertyOrderStrategy.REVERSE)));

    /**
     * If enabled, Quarkus will a create a JAX-RS resolves that configures JSON-B with the properties
     * specified here
     * It will also attempt to generate serializers for JAX-RS return types
     */
    @ConfigItem(defaultValue = "false")
    boolean enabled;

    /**
     * default locale to use
     */
    @ConfigItem
    Optional<String> locale;

    /**
     * default date format to use
     */
    @ConfigItem
    Optional<String> dateFormat;

    /**
     * defines whether or not null values are serialized
     */
    @ConfigItem(defaultValue = "false")
    boolean serializeNullValues;

    /**
     * defines the order in which the properties appear in the json output
     */
    @ConfigItem(defaultValue = PropertyOrderStrategy.LEXICOGRAPHICAL)
    String propertyOrderStrategy;

    // DESERIALIZER RELATED PROPERTIES

    /**
     * encoding to use when de-serializing data
     */
    @ConfigItem
    Optional<String> encoding;

    /**
     * specified whether unknown properties will cause deserialization to fail
     */
    @ConfigItem(defaultValue = "true")
    boolean failOnUnknownProperties;

    boolean isValidPropertyOrderStrategy() {
        return ALLOWED_PROPERTY_ORDER_VALUES.contains(propertyOrderStrategy.toUpperCase());
    }
}
