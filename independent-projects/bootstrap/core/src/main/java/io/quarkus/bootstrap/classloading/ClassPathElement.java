package io.quarkus.bootstrap.classloading;

import java.io.Closeable;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.EmptyPathTree;
import io.quarkus.paths.ManifestAttributes;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathTree;

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
        ResolvedDependency resolvedDependency = getResolvedDependency();
        return resolvedDependency != null ? resolvedDependency.getKey() : null;
    }

    /**
     * If this classpath element represents a Maven artifact, the method will return it,
     * otherwise - null.
     *
     * @return the Maven artifact this classpath element represents or null, in case
     *         this element does not represent any Maven artifact
     */
    default ResolvedDependency getResolvedDependency() {
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
     * Whether this class path element contains resources that can be reloaded in dev mode.
     */
    boolean containsReloadableResources();

    /**
     *
     * @return The protection domain that should be used to define classes from this element
     */
    ProtectionDomain getProtectionDomain();

    ManifestAttributes getManifestAttributes();

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
        return fromDependency(dep.getContentTree(), dep);
    }

    static ClassPathElement fromDependency(PathTree contentTree, ResolvedDependency dep) {
        return new PathTreeClassPathElement(contentTree, dep.isRuntimeCp(), dep);
    }

    ClassPathElement EMPTY = new ClassPathElement() {

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
            return Set.of();
        }

        @Override
        public boolean containsReloadableResources() {
            return false;
        }

        @Override
        public ProtectionDomain getProtectionDomain() {
            return null;
        }

        @Override
        public ManifestAttributes getManifestAttributes() {
            return null;
        }

        @Override
        public void close() {

        }
    };

    default List<ClassPathResource> getResources(String name) {
        ClassPathResource resource = getResource(name);
        return resource == null ? List.of() : List.of(resource);
    }
}
