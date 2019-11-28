package io.quarkus.runner.classloading;

import java.io.Closeable;
import java.security.ProtectionDomain;
import java.util.Set;

/**
 * Represents an element on the virtual classpath, such as a jar file or classes
 * directory.
 */
public interface ClassPathElement extends Closeable {

    /**
     * Loads a resource from the class path element, or null if it does not exist.
     *
     * @param name The resource to load
     * @return An representation of the class path resource if it exists
     */
    ClassPathResource getResource(String name);

    /**
     * Returns a set of all known resources.
     *
     * @return A set representing all known resources
     */
    Set<String> getProvidedResources();

    /**
     *
     * @return The protection domain that should be used to define classes from this element
     */
    ProtectionDomain getProtectionDomain(ClassLoader classLoader);
}
