package io.quarkus.maven.dependency;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.Mappable;
import io.quarkus.bootstrap.model.MappableCollectionFactory;

public class ArtifactDependency extends GACTV implements Dependency, Serializable {

    private static final long serialVersionUID = 5669341172899612719L;

    @Deprecated(forRemoval = true)
    public static Dependency of(String groupId, String artifactId, String version) {
        return new ArtifactDependency(groupId, artifactId, null, ArtifactCoords.TYPE_JAR, version);
    }

    static void putInMap(Dependency dependency, Map<String, Object> map, MappableCollectionFactory factory) {
        map.put(BootstrapConstants.MAPPABLE_MAVEN_ARTIFACT, dependency.toGACTVString());
        map.put(BootstrapConstants.MAPPABLE_SCOPE, dependency.getScope());
        map.put(BootstrapConstants.MAPPABLE_FLAGS, dependency.getFlags());
        if (!dependency.getExclusions().isEmpty()) {
            map.put(BootstrapConstants.MAPPABLE_EXCLUSIONS, Mappable.toStringCollection(dependency.getExclusions(), factory));
        }
    }

    private final String scope;
    private final int flags;
    private final Collection<ArtifactKey> exclusions;

    public ArtifactDependency(String groupId, String artifactId, String classifier, String type, String version) {
        super(groupId, artifactId, classifier, type, version);
        this.scope = SCOPE_COMPILE;
        flags = 0;
        this.exclusions = List.of();
    }

    public ArtifactDependency(String groupId, String artifactId, String classifier, String type, String version, String scope,
            boolean optional) {
        super(groupId, artifactId, classifier, type, version);
        this.scope = scope;
        flags = optional ? DependencyFlags.OPTIONAL : 0;
        this.exclusions = List.of();
    }

    public ArtifactDependency(String groupId, String artifactId, String classifier, String type, String version, int flags) {
        super(groupId, artifactId, classifier, type, version);
        this.scope = SCOPE_COMPILE;
        this.flags = flags;
        this.exclusions = List.of();
    }

    public ArtifactDependency(ArtifactCoords coords, int... flags) {
        this(coords, SCOPE_COMPILE, flags);
    }

    public ArtifactDependency(ArtifactCoords coords, String scope, int... flags) {
        super(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(), coords.getType(),
                coords.getVersion());
        this.scope = scope;
        int allFlags = 0;
        for (int f : flags) {
            allFlags |= f;
        }
        this.flags = allFlags;
        this.exclusions = List.of();
    }

    public ArtifactDependency(Dependency d) {
        super(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion());
        this.scope = d.getScope();
        this.flags = d.getFlags();
        this.exclusions = d.getExclusions();
    }

    public ArtifactDependency(AbstractDependencyBuilder<?, ?> builder) {
        super(builder.getGroupId(), builder.getArtifactId(), builder.getClassifier(), builder.getType(), builder.getVersion());
        this.scope = builder.getScope();
        this.flags = builder.getFlags();
        this.exclusions = builder.exclusions.isEmpty() ? builder.exclusions : List.copyOf(builder.exclusions);
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public int getFlags() {
        return flags;
    }

    @Override
    public Collection<ArtifactKey> getExclusions() {
        return exclusions;
    }

    @Override
    public Map<String, Object> asMap(MappableCollectionFactory factory) {
        final Map<String, Object> map = factory.newMap(4);
        putInMap(this, map, factory);
        return map;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(exclusions, flags, scope);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof ArtifactDependency))
            return false;
        ArtifactDependency other = (ArtifactDependency) obj;
        return flags == other.flags && Objects.equals(scope, other.scope) && exclusions.equals(other.exclusions);
    }

    @Override
    public String toString() {
        return "[" + toGACTVString() + " " + scope + " " + flags + "]";
    }
}
