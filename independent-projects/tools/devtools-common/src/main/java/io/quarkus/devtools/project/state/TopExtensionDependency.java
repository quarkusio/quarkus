package io.quarkus.devtools.project.state;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionOrigin;

public class TopExtensionDependency {

    public static class Builder {

        private ArtifactCoords coords;
        private ResolvedDependency resolvedDep;
        private Extension catalogMetadata;
        private boolean transitive;

        private Builder() {
        }

        public Builder setResolvedDependency(ResolvedDependency resolvedDep) {
            this.resolvedDep = resolvedDep;
            return setArtifact(new GACTV(resolvedDep.getGroupId(), resolvedDep.getArtifactId(), resolvedDep.getClassifier(),
                    resolvedDep.getType(), resolvedDep.getVersion()));
        }

        public Builder setArtifact(ArtifactCoords coords) {
            this.coords = coords;
            return this;
        }

        public Builder setCatalogMetadata(Extension metadata) {
            this.catalogMetadata = metadata;
            return this;
        }

        public Builder setTransitive(boolean transitive) {
            this.transitive = transitive;
            return this;
        }

        public TopExtensionDependency build() {
            if (catalogMetadata == null) {
                catalogMetadata = resolvedDep.getContentTree()
                        .apply(BootstrapConstants.EXTENSION_METADATA_PATH, visit -> {
                            if (visit == null) {
                                return null;
                            }
                            try {
                                return Extension.fromFile(visit.getPath());
                            } catch (IOException e) {
                                throw new UncheckedIOException(
                                        "Failed to deserialize extension metadata from " + visit.getUrl(), e);
                            }
                        });
            }
            return new TopExtensionDependency(coords, catalogMetadata, ExtensionProvider.key(getOrigin(catalogMetadata)),
                    transitive);
        }

        public ArtifactKey getKey() {
            return resolvedDep == null ? null : resolvedDep.getKey();
        }

        private static ExtensionOrigin getOrigin(Extension metadata) {
            if (metadata == null || metadata.getOrigins().isEmpty()) {
                return null;
            }
            ExtensionOrigin origin = metadata.getOrigins().get(0);
            if (origin.isPlatform()) {
                return origin;
            }
            if (metadata.getOrigins().size() > 1) {
                for (int i = 1; i < metadata.getOrigins().size(); ++i) {
                    final ExtensionOrigin o = metadata.getOrigins().get(i);
                    if (o.isPlatform()) {
                        origin = o;
                        break;
                    }
                }
            }
            return origin;
        }

    }

    public static Builder builder() {
        return new Builder();
    }

    private final ArtifactCoords coords;
    private final Extension catalogMetadata;
    private final boolean transitive;
    private final String providerKey;

    private TopExtensionDependency(ArtifactCoords coords, Extension metadata, String providerKey, boolean transitive) {
        this.coords = coords;
        this.catalogMetadata = metadata;
        this.providerKey = providerKey;
        this.transitive = transitive;
    }

    public ArtifactKey getKey() {
        return coords.getKey();
    }

    public ArtifactCoords getArtifact() {
        return coords;
    }

    public ExtensionOrigin getOrigin() {
        if (catalogMetadata == null || catalogMetadata.getOrigins().isEmpty()) {
            return null;
        }
        ExtensionOrigin origin = catalogMetadata.getOrigins().get(0);
        if (origin.isPlatform()) {
            return origin;
        }
        if (catalogMetadata.getOrigins().size() > 1) {
            for (int i = 1; i < catalogMetadata.getOrigins().size(); ++i) {
                final ExtensionOrigin o = catalogMetadata.getOrigins().get(i);
                if (o.isPlatform()) {
                    origin = o;
                    break;
                }
            }
        }
        return origin;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public String getVersion() {
        return coords.getVersion();
    }

    public String getCatalogVersion() {
        return catalogMetadata == null ? null : catalogMetadata.getArtifact().getVersion();
    }

    public boolean isNonRecommendedVersion() {
        final String catalogVersion = getCatalogVersion();
        return catalogVersion == null ? false : !catalogVersion.equals(getVersion());
    }

    public boolean isPlatformExtension() {
        final ExtensionOrigin origin = getOrigin();
        return origin == null ? false : origin.isPlatform();
    }

    public Extension getCatalogMetadata() {
        return catalogMetadata;
    }

    public String getProviderKey() {
        return providerKey;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        TopExtensionDependency that = (TopExtensionDependency) o;
        return transitive == that.transitive && Objects.equals(coords, that.coords)
                && Objects.equals(catalogMetadata, that.catalogMetadata) && Objects.equals(providerKey, that.providerKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coords, catalogMetadata, transitive, providerKey);
    }
}
