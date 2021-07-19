package io.quarkus.registry.catalog.selection;

import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ExtensionOrigins {

    public static Builder builder(ArtifactKey extKey) {
        return new ExtensionOrigins(extKey).new Builder();
    }

    public class Builder {
        private Builder() {
        }

        public Builder addOrigin(ExtensionCatalog catalog, OriginPreference preference) {
            return addOrigin(new OriginWithPreference(catalog, preference));
        }

        public Builder addOrigin(OriginWithPreference origin) {
            origins.add(origin);
            return this;
        }

        public ExtensionOrigins build() {
            origins = Collections.unmodifiableCollection(origins);
            return ExtensionOrigins.this;
        }
    }

    private final ArtifactKey extKey;
    private Collection<OriginWithPreference> origins = new ArrayList<>();

    private ExtensionOrigins(ArtifactKey extKey) {
        this.extKey = extKey;
    }

    public ArtifactKey getExtensionKey() {
        return extKey;
    }

    public Collection<OriginWithPreference> getOrigins() {
        return origins;
    }
}
