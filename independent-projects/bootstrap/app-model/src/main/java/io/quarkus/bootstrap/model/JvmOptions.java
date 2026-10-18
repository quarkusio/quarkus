package io.quarkus.bootstrap.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

/**
 * Collection of JVM arguments
 */
public interface JvmOptions extends Iterable<JvmOption> {

    /**
     * Creates a new JVM arguments builder instance.
     *
     * @return JVM arguments builder
     */
    static JvmOptionsBuilder builder() {
        return new JvmOptionsBuilder();
    }

    /**
     * Collection of JVM arguments.
     *
     * @return collection of JVM arguments
     */
    Collection<JvmOption> asCollection();

    /**
     * Iterator over the JVM arguments.
     *
     * @return iterator over the JVM arguments
     */
    @Override
    default Iterator<JvmOption> iterator() {
        return asCollection().iterator();
    }

    /**
     * Checks whether any JVM arguments have been configured.
     *
     * @return true if no argument was configured, otherwise - false
     */
    default boolean isEmpty() {
        return asCollection().isEmpty();
    }

    /**
     * Adds all the configured JVM arguments as properties.
     *
     * @param props properties to add the JVM arguments to
     */
    default void setAsExtensionDevModeProperties(Properties props) {
        for (var a : asCollection()) {
            a.addToQuarkusExtensionProperties(props);
        }
    }
}
