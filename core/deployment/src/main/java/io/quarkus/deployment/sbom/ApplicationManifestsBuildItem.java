package io.quarkus.deployment.sbom;

import java.util.Collection;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.sbom.ApplicationManifest;

/**
 * Application manifests collected in a build
 */
public final class ApplicationManifestsBuildItem extends SimpleBuildItem {

    private final Collection<ApplicationManifest> manifests;

    public ApplicationManifestsBuildItem(Collection<ApplicationManifest> manifests) {
        this.manifests = manifests;
    }

    /**
     * Application manifests from which SBOMs can be generated.
     *
     * @return collected application manifests
     */
    public Collection<ApplicationManifest> getManifests() {
        return manifests;
    }
}
