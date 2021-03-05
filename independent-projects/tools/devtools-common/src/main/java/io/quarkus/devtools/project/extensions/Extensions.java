package io.quarkus.devtools.project.extensions;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.registry.catalog.Extension;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.apache.maven.model.Dependency;

public final class Extensions {
    private Extensions() {
    }

    public static AppArtifactKey toKey(final Extension extension) {
        return new AppArtifactKey(extension.getArtifact().getGroupId(),
                extension.getArtifact().getArtifactId(),
                extension.getArtifact().getClassifier(),
                extension.getArtifact().getType());
    }

    public static AppArtifactKey toKey(final Dependency dependency) {
        return new AppArtifactKey(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(),
                dependency.getType());
    }

    public static Optional<Extension> findInList(Collection<Extension> list, final AppArtifactKey key) {
        return list.stream().filter(e -> Objects.equals(toCoords(e).getKey(), key)).findFirst();
    }

    public static AppArtifactCoords toCoords(final AppArtifactKey k, final String version) {
        return new AppArtifactCoords(k, version);
    }

    public static AppArtifactCoords toCoords(final Extension e) {
        return new AppArtifactCoords(e.getArtifact().getGroupId(),
                e.getArtifact().getArtifactId(),
                e.getArtifact().getClassifier(),
                e.getArtifact().getType(),
                e.getArtifact().getVersion());
    }

    public static AppArtifactCoords toCoords(final Dependency d, final String overrideVersion) {
        return overrideVersion(toCoords(d), overrideVersion);
    }

    public static String toGAV(AppArtifactCoords c) {
        if (c.getVersion() == null) {
            return toGA(c);
        }
        return c.getGroupId() + ":" + c.getArtifactId() + ":" + c.getVersion();
    }

    public static String toGA(AppArtifactCoords c) {
        return c.getGroupId() + ":" + c.getArtifactId();
    }

    public static String toGA(AppArtifactKey c) {
        return c.getGroupId() + ":" + c.getArtifactId();
    }

    public static String toGA(Extension e) {
        return e.getArtifact().getGroupId() + ":" + e.getArtifact().getArtifactId();
    }

    public static AppArtifactCoords stripVersion(final AppArtifactCoords coords) {
        return overrideVersion(coords, null);
    }

    public static AppArtifactCoords overrideVersion(final AppArtifactCoords coords, final String overrideVersion) {
        return new AppArtifactCoords(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(), coords.getType(),
                overrideVersion);
    }

    public static AppArtifactCoords toCoords(final Dependency d) {
        return new AppArtifactCoords(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion());
    }

}
