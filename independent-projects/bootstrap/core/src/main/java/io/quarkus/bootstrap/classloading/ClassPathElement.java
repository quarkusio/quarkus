package io.quarkus.bootstrap.classloading;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Set;
import java.util.jar.Manifest;

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

    Manifest getManifest();

    /**
     * Creates an element from a file system path
     */
    static ClassPathElement fromPath(Path path) {
        return Files.isDirectory(path) ? new DirectoryClassPathElement(path) : new JarClassPathElement(path);
    }

    static ClassPathElement EMPTY = new ClassPathElement() {
        @Override
        public ClassPathResource getResource(String name) {
            return null;
        }

        @Override
        public Set<String> getProvidedResources() {
            return Collections.emptySet();
        }

        @Override
        public ProtectionDomain getProtectionDomain(ClassLoader classLoader) {
            return null;
        }

        @Override
        public Manifest getManifest() {
            return null;
        }

        @Override
        public void close() throws IOException {

        }
    };
}
