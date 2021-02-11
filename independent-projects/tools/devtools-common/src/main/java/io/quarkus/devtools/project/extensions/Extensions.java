package io.quarkus.devtools.project.extensions;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.dependencies.Extension;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.maven.model.Dependency;

public final class Extensions {
    private Extensions() {
    }

    public static AppArtifactKey toKey(final Extension extension) {
        return toKey(extension.toDependency(false));
    }

    public static AppArtifactKey toKey(final Dependency dependency) {
        return new AppArtifactKey(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(),
                dependency.getType());
    }

    public static Optional<Extension> findInList(List<Extension> list, final AppArtifactKey key) {
        return list.stream().filter(e -> Objects.equals(toCoords(e).getKey(), key)).findFirst();
    }

    public static AppArtifactCoords toCoords(final AppArtifactKey k, final String version) {
        return new AppArtifactCoords(k, version);
    }

    public static AppArtifactCoords toCoords(final Extension e) {
        return toCoords(e.toDependency(false));
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
        return e.getGroupId() + ":" + e.getArtifactId();
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
