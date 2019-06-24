package io.quarkus.deployment.index;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.ApplicationArchiveImpl;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveBuildItem;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class ApplicationArchiveBuildStep {

    private static final Logger LOGGER = Logger.getLogger(ApplicationArchiveBuildStep.class);

    private static final String JANDEX_INDEX = "META-INF/jandex.idx";

    // At least Jandex 2.1 is needed
    private static final int REQUIRED_INDEX_VERSION = 8;

    IndexDependencyConfiguration config;

    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    static final class IndexDependencyConfiguration {
        /**
         * Artifacts on the class path that should also be indexed, which will allow classes in the index to be
         * processed by Quarkus processors
         */
        @ConfigItem(name = ConfigItem.PARENT)
        Map<String, IndexDependencyConfig> indexDependency;
    }

    @BuildStep
    ApplicationArchivesBuildItem build(ArchiveRootBuildItem root, ApplicationIndexBuildItem appindex,
            List<AdditionalApplicationArchiveMarkerBuildItem> appMarkers,
            List<AdditionalApplicationArchiveBuildItem> additionalApplicationArchiveBuildItem,
            LiveReloadBuildItem liveReloadContext) throws IOException {

        Set<String> markerFiles = new HashSet<>();
        for (AdditionalApplicationArchiveMarkerBuildItem i : appMarkers) {
            markerFiles.add(i.getFile());
        }

        IndexCache indexCache = liveReloadContext.getContextObject(IndexCache.class);
        if (indexCache == null) {
            indexCache = new IndexCache();
            liveReloadContext.setContextObject(IndexCache.class, indexCache);
        }

        List<ApplicationArchive> applicationArchives = scanForOtherIndexes(Thread.currentThread().getContextClassLoader(),
                markerFiles, root.getArchiveLocation(), additionalApplicationArchiveBuildItem, indexCache);
        return new ApplicationArchivesBuildItem(
                new ApplicationArchiveImpl(appindex.getIndex(), root.getArchiveRoot(), null, false, root.getArchiveLocation()),
                applicationArchives);
    }

    private List<ApplicationArchive> scanForOtherIndexes(ClassLoader classLoader, Set<String> applicationArchiveFiles,
            Path appRoot, List<AdditionalApplicationArchiveBuildItem> additionalApplicationArchives, IndexCache indexCache)
            throws IOException {
        Set<Path> dependenciesToIndex = new HashSet<>();
        //get paths that are included via index-dependencies
        dependenciesToIndex.addAll(getIndexDependencyPaths(classLoader));
        //get paths that are included via marker files
        Set<String> markers = new HashSet<>(applicationArchiveFiles);
        markers.add(JANDEX_INDEX);
        dependenciesToIndex.addAll(getMarkerFilePaths(classLoader, markers));

        //we don't index the application root, this is handled elsewhere
        dependenciesToIndex.remove(appRoot);

        for (AdditionalApplicationArchiveBuildItem i : additionalApplicationArchives) {
            dependenciesToIndex.add(i.getPath());
        }

        return indexPaths(dependenciesToIndex, classLoader, indexCache);
    }

    public List<Path> getIndexDependencyPaths(ClassLoader classLoader) {
        ArtifactIndex artifactIndex = new ArtifactIndex(new ClassPathArtifactResolver(classLoader));
        try {
            List<Path> ret = new ArrayList<>();

            for (Map.Entry<String, IndexDependencyConfig> entry : this.config.indexDependency.entrySet()) {
                Path path;
                if (entry.getValue().classifier.isEmpty()) {
                    path = artifactIndex.getPath(entry.getValue().groupId, entry.getValue().artifactId, null);
                } else {
                    path = artifactIndex.getPath(entry.getValue().groupId, entry.getValue().artifactId,
                            entry.getValue().classifier);
                }
                ret.add(path);
            }
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<ApplicationArchive> indexPaths(Set<Path> dependenciesToIndex, ClassLoader classLoader,
            IndexCache indexCache)
            throws IOException {
        List<ApplicationArchive> ret = new ArrayList<>();

        for (final Path dep : dependenciesToIndex) {
            LOGGER.debugf("Indexing dependency: %s", dep);
            if (Files.isDirectory(dep)) {
                IndexView indexView = handleFilePath(dep);
                ret.add(new ApplicationArchiveImpl(indexView, dep, null, false, dep));
            } else {
                IndexView index = handleJarPath(dep, indexCache);
                FileSystem fs = FileSystems.newFileSystem(dep, classLoader);
                ret.add(new ApplicationArchiveImpl(index, fs.getRootDirectories().iterator().next(), fs, true, dep));
            }
        }

        return ret;
    }

    private static Collection<? extends Path> getMarkerFilePaths(ClassLoader classLoader, Set<String> applicationArchiveFiles)
            throws IOException {
        List<Path> ret = new ArrayList<>();
        for (String file : applicationArchiveFiles) {
            Enumeration<URL> e = classLoader.getResources(file);
            while (e.hasMoreElements()) {
                URL url = e.nextElement();
                ret.add(urlToPath(url));
            }
        }
        return ret;
    }

    // package protected for testing purpose
    static Path urlToPath(URL url) {
        try {
            if (url.getProtocol().equals("jar")) {
                String jarPath = url.getPath().substring(0, url.getPath().lastIndexOf('!'));
                return Paths.get(new URI(jarPath));
            } else if (url.getProtocol().equals("file")) {
                int index = url.getPath().lastIndexOf("/META-INF");
                if (index == -1) {
                    return Paths.get(url.getPath());
                }
                String pathString = url.getPath().substring(0, index);
                Path path = Paths.get(new URI(url.getProtocol(), url.getHost(), pathString, null));
                return path;
            }
            throw new RuntimeException("Unknown URL type " + url.getProtocol());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Index handleFilePath(Path path) throws IOException {
        Path existing = path.resolve(JANDEX_INDEX);
        if (Files.exists(existing)) {
            try (FileInputStream in = new FileInputStream(existing.toFile())) {
                IndexReader reader = new IndexReader(in);
                if (reader.getIndexVersion() < REQUIRED_INDEX_VERSION) {
                    LOGGER.warnf("Re-indexing %s - at least Jandex 2.1 must be used to index an application dependency", path);
                    return indexFilePath(path);
                } else {
                    return reader.read();
                }
            }
        }
        return indexFilePath(path);
    }

    private static Index indexFilePath(Path path) throws IOException {
        Indexer indexer = new Indexer();
        try (Stream<Path> stream = Files.walk(path)) {
            stream.forEach(path1 -> {
                if (path1.toString().endsWith(".class")) {
                    try (FileInputStream in = new FileInputStream(path1.toFile())) {
                        indexer.index(in);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        return indexer.complete();
    }

    private static Index handleJarPath(Path path, IndexCache indexCache) throws IOException {
        return indexCache.cache.computeIfAbsent(path, new Function<Path, Index>() {
            @Override
            public Index apply(Path path) {
                try {
                    try (JarFile file = new JarFile(path.toFile())) {
                        ZipEntry existing = file.getEntry(JANDEX_INDEX);
                        if (existing != null) {
                            try (InputStream in = file.getInputStream(existing)) {
                                IndexReader reader = new IndexReader(in);
                                if (reader.getIndexVersion() < REQUIRED_INDEX_VERSION) {
                                    LOGGER.warnf(
                                            "Re-indexing %s - at least Jandex 2.1 must be used to index an application dependency",
                                            path);
                                    return indexJar(file);
                                } else {
                                    return reader.read();
                                }
                            }
                        }
                        return indexJar(file);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private static Index indexJar(JarFile file) throws IOException {
        Indexer indexer = new Indexer();
        Enumeration<JarEntry> e = file.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = e.nextElement();
            if (entry.getName().endsWith(".class")) {
                try (InputStream inputStream = file.getInputStream(entry)) {
                    indexer.index(inputStream);
                }
            }
        }
        return indexer.complete();
    }

    /**
     * When running in hot deployment mode we know that java archives will never change, there is no need
     * to re-index them each time. We cache them here to reduce the hot reload time.
     */
    private static final class IndexCache {

        final Map<Path, Index> cache = new HashMap<>();

    }
}
