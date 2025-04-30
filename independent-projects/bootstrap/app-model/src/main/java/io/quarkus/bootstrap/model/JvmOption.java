package io.quarkus.bootstrap.model;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * JVM option
 */
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
