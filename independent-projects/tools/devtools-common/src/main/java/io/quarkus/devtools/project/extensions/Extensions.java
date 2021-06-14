package io.quarkus.devtools.project.extensions;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.catalog.Extension;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.apache.maven.model.Dependency;

public final class Extensions {
    private Extensions() {
    }

    public static ArtifactKey toKey(final Extension extension) {
        return new ArtifactKey(extension.getArtifact().getGroupId(),
                extension.getArtifact().getArtifactId(),
                extension.getArtifact().getClassifier(),
                extension.getArtifact().getType());
    }

    public static ArtifactKey toKey(final Dependency dependency) {
        return new ArtifactKey(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(),
                dependency.getType());
    }

    public static Optional<Extension> findInList(Collection<Extension> list, final ArtifactKey key) {
        return list.stream().filter(e -> Objects.equals(e.getArtifact().getKey(), key)).findFirst();
    }

    public static ArtifactCoords toCoords(final ArtifactKey k, final String version) {
        return new ArtifactCoords(k, version);
    }

    @Deprecated
    public static ArtifactCoords toCoords(final Extension e) {
        return e.getArtifact();
    }

    public static ArtifactCoords toCoords(final Dependency d, final String overrideVersion) {
        return overrideVersion(toCoords(d), overrideVersion);
    }

    public static String toGAV(ArtifactCoords c) {
        if (c.getVersion() == null) {
            return toGA(c);
        }
        return c.getGroupId() + ":" + c.getArtifactId() + ":" + c.getVersion();
    }

    public static String toGA(ArtifactCoords c) {
        return c.getGroupId() + ":" + c.getArtifactId();
    }

    public static String toGA(ArtifactKey c) {
        return c.getGroupId() + ":" + c.getArtifactId();
    }

    public static String toGA(Extension e) {
        return e.getArtifact().getGroupId() + ":" + e.getArtifact().getArtifactId();
    }

    public static ArtifactCoords stripVersion(final ArtifactCoords coords) {
        return overrideVersion(coords, null);
    }

    public static ArtifactCoords overrideVersion(final ArtifactCoords coords, final String overrideVersion) {
        return new ArtifactCoords(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(), coords.getType(),
                overrideVersion);
    }

    public static ArtifactCoords toCoords(final Dependency d) {
        return new ArtifactCoords(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion());
    }

}
