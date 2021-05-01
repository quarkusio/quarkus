package io.quarkus.platform.catalog.compatibility;

import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.catalog.Extension;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Extension compatibility info.
 */
public class ExtensionCompatibility {

    private final Extension e;
    private final Map<ArtifactKey, Extension> conflictingExtensions;

    public ExtensionCompatibility(Extension e, Map<ArtifactKey, Extension> conflictingExtensions) {
        this.e = Objects.requireNonNull(e);
        this.conflictingExtensions = Objects.requireNonNull(conflictingExtensions);
    }

    /**
     * Extension this compatibility info belongs to.
     *
     * @return extension this compatibility info belongs to
     */
    public Extension getExtension() {
        return e;
    }

    /**
     * All the extensions that are known to be incompatible with the extension returned by {@link #getExtension()}.
     *
     * @return all the extensions known to be incompatible with the extension returned by {@link #getExtension()}
     */
    public Collection<Extension> getIncompatibleExtensions() {
        return conflictingExtensions.values();
    }

    /**
     * Checks whether an extension with the given key is incompatible with the one return by {@link #getExtension()}.
     *
     * @param extensionKey extension key to check for incompatibility
     * @return true, if the extension with the given key is incompatible with one return by {@link #getExtension()}, otherwise -
     *         false
     */
    public boolean isIncompatibleWith(ArtifactKey extensionKey) {
        return conflictingExtensions.containsKey(extensionKey);
    }
}
