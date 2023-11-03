package io.quarkus.devtools.project.state;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.registry.catalog.ExtensionOrigin;

public class ExtensionProvider {

    static String key(ExtensionOrigin origin) {
        if (origin == null) {
            return "unknown origin";
        }
        if (origin.isPlatform()) {
            return origin.getBom().getGroupId() + ':' + origin.getBom().getArtifactId();
        }
        return groupIdToHost(GACTV.fromString(origin.getId()).getGroupId());
    }

    public static String key(ArtifactCoords coords, boolean platform) {
        if (coords == null) {
            return "unknown origin";
        }
        if (platform) {
            return coords.getGroupId() + ':' + coords.getArtifactId();
        }
        return groupIdToHost(coords.getGroupId());
    }

    private static String groupIdToHost(final String groupId) {
        final StringBuilder buf = new StringBuilder();
        int i = groupId.lastIndexOf('.');
        int end = groupId.length();
        while (i > 0) {
            if (buf.length() > 0) {
                buf.append('.');
            }
            buf.append(groupId.substring(i + 1, end));
            end = i;
            i = groupId.lastIndexOf('.', i - 1);
        }
        if (buf.length() > 0) {
            buf.append('.');
        }
        buf.append(groupId.substring(0, end));
        return buf.toString();
    }

    public static class Builder {

        private ArtifactCoords coords;
        private Boolean platform;
        private ExtensionOrigin origin;
        private final Map<ArtifactKey, TopExtensionDependency> providedExtensions = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder setArtifact(ArtifactCoords coords) {
            this.coords = coords;
            return this;
        }

        public Builder setPlatform(boolean platform) {
            this.platform = platform;
            return this;
        }

        public Builder setOrigin(ExtensionOrigin origin) {
            this.origin = origin;
            return this;
        }

        public Builder addExtension(TopExtensionDependency e) {
            providedExtensions.put(e.getKey(), e);
            return this;
        }

        public ExtensionProvider build() {
            if (coords == null && origin != null) {
                coords = origin.isPlatform() ? origin.getBom() : GACTV.fromString(origin.getId());
            }
            if (platform == null) {
                platform = origin == null ? false : origin.isPlatform();
            }
            return new ExtensionProvider(coords, platform, providedExtensions);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String key;
    private final ArtifactCoords coords;
    private final boolean platform;
    private final Map<ArtifactKey, TopExtensionDependency> providedExtensions;

    private ExtensionProvider(ArtifactCoords coords, boolean platform,
            Map<ArtifactKey, TopExtensionDependency> providedExtensions) {
        this.key = key(coords, platform);
        this.coords = coords;
        this.platform = platform;
        this.providedExtensions = providedExtensions;
    }

    public String getKey() {
        return key;
    }

    public ArtifactCoords getArtifact() {
        return coords;
    }

    public Collection<TopExtensionDependency> getExtensions() {
        return providedExtensions.values();
    }

    public boolean isPlatform() {
        return platform;
    }

    public boolean isUnknown() {
        return coords == null;
    }
}
