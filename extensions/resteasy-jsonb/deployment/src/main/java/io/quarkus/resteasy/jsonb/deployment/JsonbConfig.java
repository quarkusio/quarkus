package io.quarkus.resteasy.jsonb.deployment;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
     * default locale to use
     */
    @ConfigItem(defaultValue = "")
    String locale;

    /**
     * default date format to use
     */
    @ConfigItem(defaultValue = "")
    String dateFormat;

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

    /**
     * encoding to use when deserializing data
     */
    @ConfigItem(defaultValue = "")
    String encoding;

    /**
     * specified whether unknown properties will cause deserialization to fail
     */
    @ConfigItem(defaultValue = "true")
    boolean failOnUnknownProperties;

    boolean isValidPropertyOrderStrategy() {
        return ALLOWED_PROPERTY_ORDER_VALUES.contains(propertyOrderStrategy.toUpperCase());
    }
}
