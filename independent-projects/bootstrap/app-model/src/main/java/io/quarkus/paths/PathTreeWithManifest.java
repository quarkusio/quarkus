package io.quarkus.paths;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    public static final int JAVA_VERSION;

    static {
        try {
            final String versionStr = "version";
            Object v = Runtime.class.getMethod(versionStr).invoke(null);
            List<Integer> list = (List<Integer>) v.getClass().getMethod(versionStr).invoke(v);
            JAVA_VERSION = list.get(0);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to obtain the Java version from java.lang.Runtime", e);
        }
    }

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
    public <T> T apply(String relativePath, Function<PathVisit, T> func) {
        return apply(relativePath, func, manifestEnabled);
    }

    protected abstract <T> T apply(String relativePath, Function<PathVisit, T> func, boolean manifestEnabled);

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

    protected Map<String, String> getMultiReleaseMapping() {
        if (multiReleaseMapping != null) {
            return multiReleaseMapping;
        }
        final Map<String, String> mrMapping = isMultiReleaseJar()
                ? apply(META_INF_VERSIONS, MultiReleaseMappingReader.INSTANCE, false)
                : Collections.emptyMap();
        initMultiReleaseMapping(mrMapping);
        return mrMapping;
    }

    protected void initMultiReleaseMapping(final Map<String, String> mrMapping) {
        multiReleaseMapping = mrMapping;
    }

    protected String toMultiReleaseRelativePath(String relativePath) {
        if (relativePath.startsWith(META_INF)) {
            return relativePath;
        }

        return getMultiReleaseMapping().getOrDefault(relativePath, relativePath);
    }

    private static class MultiReleaseMappingReader implements Function<PathVisit, Map<String, String>> {

        private static final MultiReleaseMappingReader INSTANCE = new MultiReleaseMappingReader();

        @Override
        public Map<String, String> apply(PathVisit visit) {
            if (visit == null) {
                return Collections.emptyMap();
            }
            final Path versionsDir = visit.getPath();
            if (!Files.isDirectory(versionsDir)) {
                return Collections.emptyMap();
            }
            final Path root = visit.getPath().getRoot();
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
                    versionContentMap.put(version, new Consumer<Map<String, String>>() {
                        @Override
                        public void accept(Map<String, String> map) {
                            try (Stream<Path> versionContent = Files.walk(versionDir)) {
                                versionContent.forEach(p -> {
                                    final String relativePath = versionDir.relativize(p).toString();
                                    if (!relativePath.isEmpty()) {
                                        map.put(relativePath, root.relativize(p).toString());
                                    }
                                });
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
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
