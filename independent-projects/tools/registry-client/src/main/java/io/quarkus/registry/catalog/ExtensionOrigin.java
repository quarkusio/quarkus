package io.quarkus.registry.catalog;

import java.util.Map;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;

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

    /**
     * @return optional metadata attached to the origin
     */
    Map<String, Object> getMetadata();

    default Mutable mutable() {
        return new ExtensionOriginImpl.Builder(this);
    }

    interface Mutable extends ExtensionOrigin, JsonBuilder<ExtensionOrigin> {

        Mutable setId(String id);

        Mutable setPlatform(boolean platform);

        Mutable setBom(ArtifactCoords bom);

        Mutable setMetadata(Map<String, Object> metadata);

        Mutable setMetadata(String name, Object value);

        Mutable removeMetadata(String key);

        ExtensionOrigin build();
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new ExtensionOriginImpl.Builder();
    }
}
