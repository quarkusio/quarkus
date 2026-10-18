package io.quarkus.paths;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

public abstract class PathTreeWithManifest implements PathTree {

    private static final String META_INF = "META-INF/";
    private static final String META_INF_VERSIONS = META_INF + "versions/";
    /**
     * The feature-release version of the current Java runtime (e.g., 17, 21).
     * Used to determine which {@code META-INF/versions/<N>} entries apply
     * when resolving multi-release JAR resources.
     */
    public static final int JAVA_VERSION = Runtime.version().feature();

    protected boolean manifestEnabled;
    private final ReentrantReadWriteLock manifestInfoLock = new ReentrantReadWriteLock();
    private transient ManifestAttributes manifestAttributes;
    protected transient boolean manifestInitialized;
    protected volatile Map<String, String> multiReleaseMapping;

    protected PathTreeWithManifest() {
        this(true);
    }

    protected PathTreeWithManifest(boolean manifestEnabled) {
        this.manifestEnabled = manifestEnabled;
        if (!manifestEnabled) {
            manifestInitialized = true;
            multiReleaseMapping = Collections.emptyMap();
        }
    }

    protected PathTreeWithManifest(PathTreeWithManifest pathTreeWithManifest) {
        pathTreeWithManifest.manifestReadLock().lock();
        try {
            this.manifestAttributes = pathTreeWithManifest.manifestAttributes;
            this.manifestInitialized = pathTreeWithManifest.manifestInitialized;
            this.multiReleaseMapping = pathTreeWithManifest.multiReleaseMapping;
        } finally {
            pathTreeWithManifest.manifestReadLock().unlock();
        }
        this.manifestEnabled = pathTreeWithManifest.manifestEnabled;
    }

    @Override
    public <T> T apply(String resourceName, Function<PathVisit, T> func) {
        return apply(resourceName, func, manifestEnabled);
    }

    protected abstract <T> T apply(String resourceName, Function<PathVisit, T> func, boolean manifestEnabled);

    @Override
    public ManifestAttributes getManifestAttributes() {
        // Optimistically try with a lock that allows concurrent access first, for performance.
        manifestReadLock().lock();
        try {
            if (manifestInitialized) {
                return manifestAttributes;
            }
        } finally {
            manifestReadLock().unlock();
        }
        // Failing that, try with a lock that does not allow concurrent access, to initialize the manifest.
        manifestWriteLock().lock();
        try {
            if (manifestInitialized) {
                // Someone else got here between our call to manifestReadLock().unlock()
                // and our call to manifestWriteLock().lock(); it can happen.
                return manifestAttributes;
            }
            final Manifest m = apply("META-INF/MANIFEST.MF", ManifestReader.INSTANCE, false);
            initManifest(m);
        } finally {
            manifestWriteLock().unlock();
        }
        return manifestAttributes;
    }

    protected void initManifest(Manifest m) {
        manifestAttributes = ManifestAttributes.of(m);
        manifestInitialized = true;
    }

    protected WriteLock manifestWriteLock() {
        return manifestInfoLock.writeLock();
    }

    protected ReadLock manifestReadLock() {
        return manifestInfoLock.readLock();
    }

    public boolean isMultiReleaseJar() {
        ManifestAttributes manifestAttributes = getManifestAttributes();

        return manifestAttributes != null && manifestAttributes.isMultiRelease();
    }

    /**
     * Returns a mapping of classpath resource names to the corresponding version-specific
     * resource names for resources found under {@code META-INF/versions/} in a multi-release JAR.
     * <p>
     * For example, in a multi-release JAR containing {@code META-INF/versions/11/com/example/Foo.class},
     * running on Java 11 or later would produce a mapping from {@code com/example/Foo.class}
     * to {@code META-INF/versions/11/com/example/Foo.class}.
     * <p>
     * When multiple version-specific entries exist for the same resource, the highest version
     * that does not exceed {@link #JAVA_VERSION} takes precedence.
     *
     * @return a map from classpath resource names to their version-specific resource names,
     *         or an empty map if this is not a multi-release JAR
     */
    protected Map<String, String> getMultiReleaseMapping() {
        if (multiReleaseMapping != null) {
            return multiReleaseMapping;
        }
        final Map<String, String> mrMapping = isMultiReleaseJar()
                ? apply(META_INF_VERSIONS, MultiReleaseResourceMapping.INSTANCE, false)
                : Map.of();
        initMultiReleaseMapping(mrMapping);
        return mrMapping;
    }

    protected void initMultiReleaseMapping(final Map<String, String> mrMapping) {
        multiReleaseMapping = mrMapping;
    }

    protected String toMultiReleaseResourceName(String resourceName) {
        if (resourceName.startsWith(META_INF)) {
            return resourceName;
        }

        return getMultiReleaseMapping().getOrDefault(resourceName, resourceName);
    }

    private static class MultiReleaseResourceMapping implements Function<PathVisit, Map<String, String>> {

        private static final MultiReleaseResourceMapping INSTANCE = new MultiReleaseResourceMapping();

        @Override
        public Map<String, String> apply(PathVisit visit) {
            if (visit == null) {
                return Map.of();
            }
            final Path versionsDir = visit.getPath();
            // get the root of the tree
            final Path root = getTreeRootForVersionsDir(visit.getPath());
            if (root == null) {
                return Map.of();
            }
            final TreeMap<Integer, Consumer<Map<String, String>>> versionContentMap = new TreeMap<>();
            try (Stream<Path> versions = Files.list(versionsDir)) {
                versions.forEach(versionDir -> {
                    if (!Files.isDirectory(versionDir)) {
                        return;
                    }
                    final int version;
                    try {
                        version = Integer.parseInt(versionDir.getFileName().toString());
                        if (version > JAVA_VERSION) {
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Logger.getLogger(PathTreeWithManifest.class)
                                .debug("Failed to parse " + versionDir + " entry", e);
                        return;
                    }
                    versionContentMap.put(version, map -> {
                        try (Stream<Path> versionContent = Files.walk(versionDir)) {
                            versionContent.forEach(p -> {
                                final String resourceName = asResourceName(versionDir, p);
                                if (!resourceName.isEmpty()) {
                                    map.put(resourceName, asResourceName(root, p));
                                }
                            });
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });

                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            final Map<String, String> multiReleaseMapping = new HashMap<>();
            for (Consumer<Map<String, String>> c : versionContentMap.values()) {
                c.accept(multiReleaseMapping);
            }
            return multiReleaseMapping;
        }

        private static Path getTreeRootForVersionsDir(Path versionsDir) {
            if (Files.isDirectory(versionsDir)) {
                Path parent = versionsDir.getParent();
                return parent == null ? null : parent.getParent();
            }
            return null;
        }

        private static String asResourceName(Path parentDir, Path child) {
            if (parentDir.getNameCount() == child.getNameCount()) {
                return "";
            }
            var sb = new StringBuilder().append(child.getName(parentDir.getNameCount()));
            for (int i = parentDir.getNameCount() + 1; i < child.getNameCount(); i++) {
                sb.append('/').append(child.getName(i));
            }
            return sb.toString();
        }
    }

    private static class ManifestReader implements Function<PathVisit, Manifest> {
        private static final ManifestReader INSTANCE = new ManifestReader();

        @Override
        public Manifest apply(PathVisit visit) {
            if (visit == null) {
                return null;
            }
            try (InputStream is = Files.newInputStream(visit.getPath())) {
                return new Manifest(is);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
