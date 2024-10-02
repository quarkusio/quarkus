package io.quarkus.deployment.index;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
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
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.DirectoryPathTree;
import io.quarkus.paths.MultiRootPathTree;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathTree;
import io.quarkus.paths.PathVisit;
import io.quarkus.paths.PathVisitor;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class ApplicationArchiveBuildStep {

    private static final Logger LOGGER = Logger.getLogger(ApplicationArchiveBuildStep.class);

    IndexDependencyConfiguration config;

    /**
     * Indexing
     */
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
                    indexDependencyConfig.artifactId.orElse(null), indexDependencyConfig.classifier.orElse(null)));
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

        IndexCache indexCache = liveReloadContext.getContextObject(IndexCache.class);
        if (indexCache == null) {
            indexCache = new IndexCache();
            liveReloadContext.setContextObject(IndexCache.class, indexCache);
        }

        Map<ArtifactKey, Set<String>> removedResources = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classLoadingConfig.removedResources.entrySet()) {
            removedResources.put(new GACT(entry.getKey().split(":")), entry.getValue());
        }

        // Add resources removed from the classpath by extensions
        removedResources.putAll(curateOutcomeBuildItem.getApplicationModel().getRemovedResources());

        List<ApplicationArchive> applicationArchives = scanForOtherIndexes(buildCloseables,
                appMarkers, root, additionalApplicationArchiveBuildItem, indexDependencyBuildItems, indexCache,
                curateOutcomeBuildItem, removedResources);

        final OpenPathTree tree;
        if (root.getRootDirectories().size() == 1) {
            tree = new DirectoryPathTree(root.getRootDirectories().iterator().next());
        } else {
            final PathTree[] trees = new PathTree[root.getRootDirectories().size()];
            int i = 0;
            for (Path p : root.getRootDirectories()) {
                trees[i++] = new DirectoryPathTree(p);
            }
            tree = new MultiRootPathTree(trees);
        }

        return new ApplicationArchivesBuildItem(
                new ApplicationArchiveImpl(appindex.getIndex(), tree,
                        curateOutcomeBuildItem.getApplicationModel().getAppArtifact()),
                applicationArchives);
    }

    private List<ApplicationArchive> scanForOtherIndexes(QuarkusBuildCloseablesBuildItem buildCloseables,
            List<AdditionalApplicationArchiveMarkerBuildItem> appMarkers,
            ArchiveRootBuildItem root, List<AdditionalApplicationArchiveBuildItem> additionalApplicationArchives,
            List<IndexDependencyBuildItem> indexDependencyBuildItem, IndexCache indexCache,
            CurateOutcomeBuildItem curateOutcomeBuildItem, Map<ArtifactKey, Set<String>> removedResources)
            throws IOException {

        List<ApplicationArchive> appArchives = new ArrayList<>();
        Set<Path> indexedPaths = new HashSet<>();

        //get paths that are included via marker files
        final Set<String> markers = new HashSet<>(appMarkers.size() + 1);
        for (AdditionalApplicationArchiveMarkerBuildItem i : appMarkers) {
            final String marker = i.getFile();
            markers.add(marker.endsWith("/") ? marker.substring(0, marker.length() - 1) : marker);
        }
        markers.add(IndexingUtil.JANDEX_INDEX);
        addMarkerFilePaths(markers, root, indexedPaths, appArchives, indexCache, removedResources);

        //get paths that are included via index-dependencies
        addIndexDependencyPaths(indexDependencyBuildItem, root, indexedPaths, appArchives, buildCloseables,
                indexCache, curateOutcomeBuildItem, removedResources);

        for (AdditionalApplicationArchiveBuildItem i : additionalApplicationArchives) {
            for (Path apPath : i.getResolvedPaths()) {
                if (!root.getResolvedPaths().contains(apPath) && indexedPaths.add(apPath)) {
                    appArchives.add(createApplicationArchive(buildCloseables, indexCache, apPath, null,
                            removedResources));
                }
            }
        }

        return appArchives;
    }

    private void addIndexDependencyPaths(List<IndexDependencyBuildItem> indexDependencyBuildItems, ArchiveRootBuildItem root,
            Set<Path> indexedDeps, List<ApplicationArchive> appArchives,
            QuarkusBuildCloseablesBuildItem buildCloseables, IndexCache indexCache,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            Map<ArtifactKey, Set<String>> removedResources) {
        if (indexDependencyBuildItems.isEmpty()) {
            return;
        }
        final Set<ArtifactKey> indexDependencyKeys = new HashSet<>();
        final Set<String> indexGroupIds = new HashSet<>();
        for (IndexDependencyBuildItem indexDependencyBuildItem : indexDependencyBuildItems) {
            if (indexDependencyBuildItem.getArtifactId() != null) {
                indexDependencyKeys.add(ArtifactKey.of(indexDependencyBuildItem.getGroupId(),
                        indexDependencyBuildItem.getArtifactId(),
                        indexDependencyBuildItem.getClassifier(),
                        ArtifactCoords.TYPE_JAR));
            } else {
                indexGroupIds.add(indexDependencyBuildItem.getGroupId());
            }
        }
        for (ResolvedDependency dep : curateOutcomeBuildItem.getApplicationModel().getDependencies()) {
            if (dep.isRuntimeCp()
                    && (indexDependencyKeys.contains(dep.getKey()) || indexGroupIds.contains(dep.getGroupId()))) {
                for (Path path : dep.getContentTree().getRoots()) {
                    if (!root.isExcludedFromIndexing(path)
                            && !root.getResolvedPaths().contains(path)
                            && indexedDeps.add(path)) {
                        try {
                            appArchives.add(createApplicationArchive(buildCloseables, indexCache, path, dep,
                                    removedResources));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                }
            }
        }
    }

    private static ApplicationArchive createApplicationArchive(QuarkusBuildCloseablesBuildItem buildCloseables,
            IndexCache indexCache, Path dep, ResolvedDependency resolvedDependency,
            Map<ArtifactKey, Set<String>> removedResources)
            throws IOException {
        LOGGER.debugf("Indexing dependency: %s", dep);
        final Set<String> removed = resolvedDependency != null ? removedResources.get(resolvedDependency.getKey()) : null;
        final OpenPathTree openTree;
        final IndexView index;
        if (Files.isDirectory(dep)) {
            openTree = new DirectoryPathTree(dep);
            index = indexPathTree(openTree, removed);
        } else {
            openTree = buildCloseables.add(PathTree.ofArchive(dep).open());
            index = handleJarPath(dep, indexCache, removed);
        }
        return new ApplicationArchiveImpl(index, openTree, resolvedDependency);
    }

    private static void addMarkerFilePaths(Set<String> applicationArchiveMarkers,
            ArchiveRootBuildItem root, Set<Path> indexedPaths, List<ApplicationArchive> appArchives,
            IndexCache indexCache, Map<ArtifactKey, Set<String>> removed)
            throws IOException {
        final QuarkusClassLoader cl = ((QuarkusClassLoader) Thread.currentThread().getContextClassLoader());
        final Set<ArtifactKey> indexedElements = new HashSet<>();
        for (String marker : applicationArchiveMarkers) {
            final List<ClassPathElement> elements = cl.getElementsWithResource(marker, false);
            if (elements.isEmpty()) {
                continue;
            }
            for (ClassPathElement cpe : elements) {
                if (!cpe.isRuntime()) {
                    continue;
                }
                final ArtifactKey dependencyKey = cpe.getDependencyKey();
                if (dependencyKey == null || !indexedElements.add(dependencyKey)) {
                    continue;
                }
                cpe.apply(tree -> {
                    indexedPaths.addAll(tree.getOriginalTree().getRoots());

                    final Path rootPath = tree.getOriginalTree().getRoots().size() == 1
                            ? tree.getOriginalTree().getRoots().iterator().next()
                            : null;
                    if (rootPath != null && !Files.isDirectory(rootPath)) {
                        if (root.isExcludedFromIndexing(rootPath)) {
                            return null;
                        }
                        Index index = indexCache.cache.get(rootPath);
                        if (index == null) {
                            try {
                                index = IndexingUtil.indexTree(tree, removed.get(dependencyKey));
                            } catch (IOException ioe) {
                                throw new UncheckedIOException(ioe);
                            }
                            indexCache.cache.put(rootPath, index);
                        }
                        appArchives.add(new ApplicationArchiveImpl(index, tree, cpe.getResolvedDependency()));
                        return null;
                    }

                    final ApplicationArchive archive = tree.apply(marker, visit -> {
                        if (visit == null || root.isExcludedFromIndexing(visit.getRoot())) {
                            return null;
                        }
                        final Index index;
                        try {
                            index = indexPathTree(tree, removed.get(dependencyKey));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        return new ApplicationArchiveImpl(index, tree, cpe.getResolvedDependency());
                    });
                    if (archive != null) {
                        appArchives.add(archive);
                    }
                    return null;
                });
            }
        }
    }

    private static Index indexPathTree(PathTree tree, Set<String> removed) throws IOException {
        Indexer indexer = new Indexer();
        tree.walk(new PathVisitor() {
            @Override
            public void visitPath(PathVisit visit) {
                final Path path = visit.getPath();
                final Path fileName = path.getFileName();
                if (fileName == null
                        || !fileName.toString().endsWith(".class")
                        || Files.isDirectory(path)
                        || removed != null && removed.contains(visit.getRelativePath("/"))) {
                    return;
                }
                try (InputStream in = Files.newInputStream(path)) {
                    indexer.index(in);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
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
