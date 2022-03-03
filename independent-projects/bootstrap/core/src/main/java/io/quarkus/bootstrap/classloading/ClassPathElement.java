package io.quarkus.bootstrap.classloading;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.EmptyPathTree;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathTree;
import java.io.Closeable;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Manifest;

/**
 * Represents an element on the virtual classpath, such as a jar file or classes
 * directory.
 */
public interface ClassPathElement extends Closeable {

    /**
     * If this classpath element represents a Maven artifact, the method will return its key,
     * otherwise - null.
     *
     * @return the key of the Maven artifact this classpath element represents or null, in case
     *         this element does not represent any Maven artifact
     */
    default ArtifactKey getDependencyKey() {
        return null;
    }

    /**
     *
     * @return The element root, or null if not applicable
     */
    Path getRoot();

    /**
     * Processes the content of this classpath element and returns a result.
     *
     * @param <T> result type
     * @param func content processing function
     * @return processing result
     */
    <T> T apply(Function<OpenPathTree, T> func);

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
     * Checks whether this is a runtime classpath element
     *
     * @return true in case this is a runtime classpath element, otherwise - false
     */
    boolean isRuntime();

    /**
     * Creates an element from a file system path
     */
    static ClassPathElement fromPath(Path path, boolean runtime) {
        return new PathTreeClassPathElement(PathTree.ofDirectoryOrArchive(path),
                runtime);
    }

    static ClassPathElement fromDependency(ResolvedDependency dep) {
        return new PathTreeClassPathElement(dep.getContentTree(), dep.isRuntimeCp(), dep.getKey());
    }

    static ClassPathElement EMPTY = new ClassPathElement() {
        @Override
        public Path getRoot() {
            return null;
        }

        @Override
        public boolean isRuntime() {
            return false;
        }

        @Override
        public <T> T apply(Function<OpenPathTree, T> func) {
            return func.apply(EmptyPathTree.getInstance());
        }

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
        public void close() {

        }
    };
}
