package io.quarkus.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.jandex.IndexView;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;

public final class ApplicationArchiveImpl extends MultiBuildItem implements ApplicationArchive {

    private final IndexView indexView;
    private final OpenPathTree openTree;
    private final ResolvedDependency resolvedDependency;

    public ApplicationArchiveImpl(IndexView indexView, OpenPathTree openTree, ResolvedDependency resolvedDependency) {
        this.indexView = indexView;
        this.openTree = openTree;
        this.resolvedDependency = resolvedDependency;
    }

    @Override
    public IndexView getIndex() {
        return indexView;
    }

    @Override
    @Deprecated
    public Path getArchiveLocation() {
        return openTree.getOriginalTree().getRoots().iterator().next();
    }

    @Override
    @Deprecated
    public PathsCollection getRootDirs() {
        return PathsCollection.from(openTree.getRoots());
    }

    @Override
    public PathCollection getRootDirectories() {
        return PathList.from(openTree.getRoots());
    }

    @Override
    @Deprecated
    public PathsCollection getPaths() {
        return PathsCollection.from(openTree.getOriginalTree().getRoots());
    }

    @Override
    public PathCollection getResolvedPaths() {
        return PathList.from(openTree.getOriginalTree().getRoots());
    }

    @Override
    @Deprecated
    /**
     * @deprecated in favor of {@link #getKey()}
     * @return archive key
     */
    public AppArtifactKey getArtifactKey() {
        if (resolvedDependency == null) {
            return null;
        }
        ArtifactKey artifactKey = resolvedDependency.getKey();
        return new AppArtifactKey(artifactKey.getGroupId(), artifactKey.getArtifactId(), artifactKey.getClassifier(),
                artifactKey.getType());
    }

    @Override
    public ArtifactKey getKey() {
        return resolvedDependency != null ? resolvedDependency.getKey() : null;
    }

    @Override
    public ResolvedDependency getResolvedDependency() {
        return resolvedDependency;
    }

    @Override
    public <T> T apply(Function<OpenPathTree, T> func) {
        if (openTree.isOpen()) {
            try {
                return func.apply(openTree);
            } catch (Exception e) {
                if (openTree.isOpen()) {
                    throw e;
                }
            }
        }
        try (OpenPathTree openTree = this.openTree.getOriginalTree().open()) {
            return func.apply(openTree);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open path tree with root " + openTree.getOriginalTree().getRoots(), e);
        }
    }

    @Override
    public void accept(Consumer<OpenPathTree> func) {
        if (openTree.isOpen()) {
            try {
                func.accept(openTree);
                return;
            } catch (Exception e) {
                if (openTree.isOpen()) {
                    throw e;
                }
            }
        }
        try (OpenPathTree openTree = this.openTree.getOriginalTree().open()) {
            func.accept(openTree);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open path tree with root " + openTree.getOriginalTree().getRoots(), e);
        }
    }
}
