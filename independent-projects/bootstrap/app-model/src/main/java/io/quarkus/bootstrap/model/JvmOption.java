package io.quarkus.bootstrap.model;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * JVM option
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", defaultImpl = Void.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MutableStandardJvmOption.class, name = "standard"),
        @JsonSubTypes.Type(value = MutableXxJvmOption.class, name = "xx"),
})
public interface JvmOption {

    /**
     * Simple option name without dashes
     *
     * @return simple option name without dashes
     */
    String getName();

    /**
     * Checks whether an option has a value
     *
     * @return true if an option has a value, otherwise - false
     */
    default boolean hasValue() {
        return !getValues().isEmpty();
    }

    /**
     * All the configured option values.
     *
     * @return all the configured values
     */
    Collection<String> getValues();

    /**
     * Adds an option with its values as a property.
     *
     * @param props properties to add an argument to
     */
    void addToQuarkusExtensionProperties(Properties props);

    /**
     * Returns command line representation of this option.
     *
     * @return Java command line representation of this option
     */
    List<String> toCliOptions();
}
