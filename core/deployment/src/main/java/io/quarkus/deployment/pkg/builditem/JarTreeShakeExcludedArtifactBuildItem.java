package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Excludes an entire dependency artifact from jar tree-shake class removal.
 * All classes from excluded artifacts are treated as reachable and will never
 * be removed by the tree-shaker.
 * <p>
 * This is needed for libraries like BouncyCastle FIPS that perform self-integrity
 * checks (checksums over their own classes) and break if any classes are removed.
 */
public final class JarTreeShakeExcludedArtifactBuildItem extends MultiBuildItem {

    private final ArtifactKey artifactKey;

    public JarTreeShakeExcludedArtifactBuildItem(ArtifactKey artifactKey) {
        this.artifactKey = artifactKey;
    }

    public ArtifactKey getArtifactKey() {
        return artifactKey;
    }
}
