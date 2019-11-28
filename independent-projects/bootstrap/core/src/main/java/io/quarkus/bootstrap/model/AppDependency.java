package io.quarkus.bootstrap.model;

import java.io.Serializable;

/**
 * Represents an application artifact dependency.
 *
 * @author Alexey Loubyansky
 */
public class AppDependency implements Serializable {

    private final AppArtifact artifact;
    private final String scope;
    private final boolean optional;

    public AppDependency(AppArtifact artifact, String scope) {
        this(artifact, scope, false);
    }

    public AppDependency(AppArtifact artifact, String scope, boolean optional) {
        this.artifact = artifact;
        this.scope = scope;
        this.optional = optional;
    }

    public AppArtifact getArtifact() {
        return artifact;
    }

    public String getScope() {
        return scope;
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifact == null) ? 0 : artifact.hashCode());
        result = prime * result + (optional ? 1231 : 1237);
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AppDependency other = (AppDependency) obj;
        if (artifact == null) {
            if (other.artifact != null)
                return false;
        } else if (!artifact.equals(other.artifact))
            return false;
        if (optional != other.optional)
            return false;
        if (scope == null) {
            if (other.scope != null)
                return false;
        } else if (!scope.equals(other.scope))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        artifact.append(buf).append('(').append(scope);
        if(optional) {
            buf.append(" optional");
        }
        return buf.append(')').toString();
    }
}
