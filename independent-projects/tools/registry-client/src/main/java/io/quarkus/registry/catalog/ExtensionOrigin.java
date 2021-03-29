package io.quarkus.registry.catalog;

import io.quarkus.maven.ArtifactCoords;

public interface ExtensionOrigin {

    /**
     * Origin ID. E.g. GAV of the descriptor.
     *
     * @return origin ID
     */
    String getId();

    /**
     * BOM that should be imported by a project
     * using extensions from this origin. This method normally won't return null.
     * Given that any Quarkus project would typically be importing at least some version of
     * io.quarkus:quarkus-bom even if extensions used in the project aren't managed by the quarkus-bom/
     * the project
     *
     * @return BOM coordinates
     */
    ArtifactCoords getBom();

    /**
     * Whether the origin represents a platform.
     *
     * @return true in case the origin is a platform, otherwise - false
     */
    boolean isPlatform();
}
