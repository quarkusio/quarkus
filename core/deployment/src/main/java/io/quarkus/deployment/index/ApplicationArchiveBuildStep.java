package io.quarkus.deployment.index;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.ApplicationArchiveImpl;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveBuildItem;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.QuarkusBuildCloseablesBuildItem;
import io.quarkus.deployment.configuration.ClassLoadingConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class ApplicationArchiveBuildStep {

    private static final Logger LOGGER = Logger.getLogger(ApplicationArchiveBuildStep.class);

    IndexDependencyConfiguration config;

    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    static final class IndexDependencyConfiguration {
        /**
         * Artifacts on the classpath that should also be indexed.
         * <p>
         * Their classes will be in the index and processed by Quarkus processors.
         */
        @ConfigItem(name = ConfigItem.PARENT)
        @ConfigDocSection
        @ConfigDocMapKey("dependency-name")
        Map<String, IndexDependencyConfig> indexDependency;
    }

    @BuildStep
    void addConfiguredIndexedDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencyBuildItemBuildProducer) {
        for (IndexDependencyConfig indexDependencyConfig : config.indexDependency.values()) {
            indexDependencyBuildItemBuildProducer.produce(new IndexDependencyBuildItem(indexDependencyConfig.groupId,
                    indexDependencyConfig.artifactId, indexDependencyConfig.classifier.orElse(null)));
        }
    }

    @BuildStep
    ApplicationArchivesBuildItem build(
            QuarkusBuildCloseablesBuildItem buildCloseables,
            ArchiveRootBuildItem root, ApplicationIndexBuildItem appindex,
            List<AdditionalApplicationArchiveMarkerBuildItem> appMarkers,
            List<AdditionalApplicationArchiveBuildItem> additionalApplicationArchiveBuildItem,
            List<IndexDependencyBuildItem> indexDependencyBuildItems,
            LiveReloadBuildItem liveReloadContext,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            ClassLoadingConfig classLoadingConfig) throws IOException {

        Set<String> markerFiles = new HashSet<>();
        for (AdditionalApplicationArchiveMarkerBuildItem i : appMarkers) {
            markerFiles.add(i.getFile());
        }

        IndexCache indexCache = liveReloadContext.getContextObject(IndexCache.class);
        if (indexCache == null) {
            indexCache = new IndexCache();
            liveReloadContext.setContextObject(IndexCache.class, indexCache);
        }

        Map<AppArtifactKey, Set<String>> removedResources = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classLoadingConfig.removedResources.entrySet()) {
            removedResources.put(new AppArtifactKey(entry.getKey().split(":")), entry.getValue());
        }

        List<ApplicationArchive> applicationArchives = scanForOtherIndexes(buildCloseables,
                Thread.currentThread().getContextClassLoader(),
                markerFiles, root, additionalApplicationArchiveBuildItem, indexDependencyBuildItems, indexCache,
                curateOutcomeBuildItem, removedResources);
        return new ApplicationArchivesBuildItem(
                new ApplicationArchiveImpl(appindex.getIndex(), root.getRootDirs(), root.getPaths(), null),
                applicationArchives);
    }

    private List<ApplicationArchive> scanForOtherIndexes(QuarkusBuildCloseablesBuildItem buildCloseables,
            ClassLoader classLoader, Set<String> applicationArchiveFiles,
            ArchiveRootBuildItem root, List<AdditionalApplicationArchiveBuildItem> additionalApplicationArchives,
            List<IndexDependencyBuildItem> indexDependencyBuildItem, IndexCache indexCache,
            CurateOutcomeBuildItem curateOutcomeBuildItem, Map<AppArtifactKey, Set<String>> removedResources)
            throws IOException {

        List<ApplicationArchive> appArchives = new ArrayList<>();
        Set<Path> indexedPaths = new HashSet<>();

        //get paths that are included via marker files
        Set<String> markers = new HashSet<>(applicationArchiveFiles);
        markers.add(IndexingUtil.JANDEX_INDEX);
        addMarkerFilePaths(markers, root, curateOutcomeBuildItem, indexedPaths, appArchives, buildCloseables, classLoader,
                indexCache, removedResources);

        //get paths that are included via index-dependencies
        addIndexDependencyPaths(indexDependencyBuildItem, classLoader, root, indexedPaths, appArchives, buildCloseables,
                indexCache, curateOutcomeBuildItem, removedResources);

        for (AdditionalApplicationArchiveBuildItem i : additionalApplicationArchives) {
            for (Path apPath : i.getPaths()) {
                if (!root.getPaths().contains(apPath) && indexedPaths.add(apPath)) {
                    appArchives.add(createApplicationArchive(buildCloseables, classLoader, indexCache, apPath, null,
                            removedResources));
                }
            }
        }

        return appArchives;
    }

    public void addIndexDependencyPaths(List<IndexDependencyBuildItem> indexDependencyBuildItems,
            ClassLoader classLoader, ArchiveRootBuildItem root, Set<Path> indexedDeps, List<ApplicationArchive> appArchives,
            QuarkusBuildCloseablesBuildItem buildCloseables, IndexCache indexCache,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            Map<AppArtifactKey, Set<String>> removedResources) {
        if (indexDependencyBuildItems.isEmpty()) {
            return;
        }
        final List<AppDependency> userDeps = curateOutcomeBuildItem.getEffectiveModel().getUserDependencies();
        final Map<AppArtifactKey, AppArtifact> userMap = new HashMap<>(userDeps.size());
        for (AppDependency dep : userDeps) {
            userMap.put(dep.getArtifact().getKey(), dep.getArtifact());
        }
        try {
            for (IndexDependencyBuildItem indexDependencyBuildItem : indexDependencyBuildItems) {
                final AppArtifactKey key = new AppArtifactKey(indexDependencyBuildItem.getGroupId(),
                        indexDependencyBuildItem.getArtifactId(),
                        indexDependencyBuildItem.getClassifier(),
                        "jar");
                final AppArtifact artifact = userMap.get(key);
                if (artifact == null) {
                    throw new RuntimeException(
                            "Could not resolve artifact " + key + " among the runtime dependencies of the application");
                }
                for (Path path : artifact.getPaths()) {
                    if (!root.isExcludedFromIndexing(path) && !root.getPaths().contains(path) && indexedDeps.add(path)) {
                        appArchives.add(createApplicationArchive(buildCloseables, classLoader, indexCache, path, key,
                                removedResources));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ApplicationArchive createApplicationArchive(QuarkusBuildCloseablesBuildItem buildCloseables,
            ClassLoader classLoader,
            IndexCache indexCache, Path dep, AppArtifactKey artifactKey, Map<AppArtifactKey, Set<String>> removedResources)
            throws IOException {
        final FileSystem fs = Files.isDirectory(dep) ? null : buildCloseables.add(FileSystems.newFileSystem(dep, classLoader));
        Set<String> removed = removedResources.get(artifactKey);
        return new ApplicationArchiveImpl(indexPath(indexCache, dep, removed),
                fs == null ? dep : fs.getRootDirectories().iterator().next(), fs, fs != null, dep, artifactKey);
    }

    private static IndexView indexPath(IndexCache indexCache, Path dep, Set<String> removed) throws IOException {
        LOGGER.debugf("Indexing dependency: %s", dep);
        return Files.isDirectory(dep) ? handleFilePath(dep, removed) : handleJarPath(dep, indexCache, removed);
    }

    private static void addMarkerFilePaths(Set<String> applicationArchiveFiles,
            ArchiveRootBuildItem root, CurateOutcomeBuildItem curateOutcomeBuildItem, Set<Path> indexedPaths,
            List<ApplicationArchive> appArchives, QuarkusBuildCloseablesBuildItem buildCloseables, ClassLoader classLoader,
            IndexCache indexCache, Map<AppArtifactKey, Set<String>> removed)
            throws IOException {
        for (AppDependency dep : curateOutcomeBuildItem.getEffectiveModel().getUserDependencies()) {
            final PathsCollection artifactPaths = dep.getArtifact().getPaths();
            boolean containsMarker = false;
            for (Path p : artifactPaths) {
                if (root.isExcludedFromIndexing(p)) {
                    continue;
                }
                if (Files.isDirectory(p)) {
                    if (containsMarker = containsMarker(p, applicationArchiveFiles)) {
                        break;
                    }
                } else {
                    try (FileSystem fs = FileSystems.newFileSystem(p, classLoader)) {
                        if (containsMarker = containsMarker(fs.getPath("/"), applicationArchiveFiles)) {
                            break;
                        }
                    } catch (ProviderNotFoundException e) {
                        // that is pretty much an exceptional case
                        // it's not a dir and not a jar, it could be anything (e.g. a pom file that
                        // ended up in some project deps)
                        // not necessarily a wrong state
                    }
                }
            }

            if (containsMarker) {
                final PathsCollection.Builder rootDirs = PathsCollection.builder();
                final List<IndexView> indexes = new ArrayList<>(artifactPaths.size());
                for (Path p : artifactPaths) {
                    if (Files.isDirectory(p)) {
                        rootDirs.add(p);
                    } else {
                        final FileSystem fs = buildCloseables.add(FileSystems.newFileSystem(p, classLoader));
                        fs.getRootDirectories().forEach(rootDirs::add);
                    }
                    indexes.add(
                            indexPath(indexCache, p, removed.get(dep.getArtifact().getKey())));

                    indexedPaths.add(p);
                }
                appArchives
                        .add(new ApplicationArchiveImpl(indexes.size() == 1 ? indexes.get(0) : CompositeIndex.create(indexes),
                                rootDirs.build(), artifactPaths, dep.getArtifact().getKey()));
            }
        }
    }

    private static boolean containsMarker(Path dir, Set<String> applicationArchiveFiles) throws IOException {
        for (String file : applicationArchiveFiles) {
            if (Files.exists(dir.resolve(file))) {
                return true;
            }
        }
        return false;
    }

    private static Index handleFilePath(Path path, Set<String> removed) throws IOException {
        return indexFilePath(path, removed);
    }

    private static Index indexFilePath(Path path, Set<String> removed) throws IOException {
        Indexer indexer = new Indexer();
        try (Stream<Path> stream = Files.walk(path)) {
            stream.forEach(path1 -> {
                if (removed != null) {
                    String relative = path.relativize(path1).toString().replace("\\", "/");
                    if (removed.contains(relative)) {
                        return;
                    }
                }
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

    private static Index handleJarPath(Path path, IndexCache indexCache, Set<String> removed) {
        return indexCache.cache.computeIfAbsent(path, new Function<Path, Index>() {
            @Override
            public Index apply(Path path) {
                try {
                    return IndexingUtil.indexJar(path, removed);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to process " + path, e);
                }
            }
        });
    }

    /**
     * When running in hot deployment mode we know that java archives will never change, there is no need
     * to re-index them each time. We cache them here to reduce the hot reload time.
     */
    private static final class IndexCache {

        final Map<Path, Index> cache = new HashMap<>();

    }
}
