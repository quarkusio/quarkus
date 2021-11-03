package io.quarkus.maven.dependency;

import java.io.Serializable;
import java.util.Objects;

public class ArtifactDependency extends GACTV implements Dependency, Serializable {

    private final String scope;
    private int flags;

    public ArtifactDependency(String groupId, String artifactId, String classifier, String type, String version) {
        super(groupId, artifactId, classifier, type, version);
        this.scope = "copmpile";
        flags = 0;
    }

    public ArtifactDependency(ArtifactCoords coords, int... flags) {
        this(coords, "compile", flags);
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
    }

    public ArtifactDependency(Dependency d) {
        super(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion());
        this.scope = d.getScope();
        this.flags = d.getFlags();
    }

    public ArtifactDependency(AbstractDependencyBuilder<?, ?> builder) {
        super(builder.getGroupId(), builder.getArtifactId(), builder.getClassifier(), builder.getType(), builder.getVersion());
        this.scope = builder.getScope();
        this.flags = builder.getFlags();
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public int getFlags() {
        return flags;
    }

    public void setFlag(int flag) {
        flags |= flag;
    }

    public void clearFlag(int flag) {
        if ((flags & flag) > 0) {
            flags ^= flag;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(flags, scope);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArtifactDependency other = (ArtifactDependency) obj;
        return flags == other.flags && Objects.equals(scope, other.scope);
    }

    @Override
    public String toString() {
        return "[" + toGACTVString() + " " + scope + "]";
    }
}
