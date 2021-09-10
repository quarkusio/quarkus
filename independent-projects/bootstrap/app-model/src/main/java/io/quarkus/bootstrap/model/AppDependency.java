package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.Objects;

public class AppDependency implements Serializable {

    public static final int OPTIONAL_FLAG = 0b000001;
    public static final int DIRECT_FLAG = 0b000010;
    public static final int RUNTIME_CP_FLAG = 0b000100;
    public static final int DEPLOYMENT_CP_FLAG = 0b001000;
    public static final int RUNTIME_EXTENSION_ARTIFACT_FLAG = 0b010000;

    private final AppArtifact artifact;
    private final String scope;
    private final int flags;

    public AppDependency(AppArtifact artifact, String scope, int... flags) {
        this(artifact, scope, false, flags);
    }

    public AppDependency(AppArtifact artifact, String scope, boolean optional, int... flags) {
        this.artifact = artifact;
        this.scope = scope;
        int tmpFlags = optional ? OPTIONAL_FLAG : 0;
        for (int f : flags) {
            tmpFlags |= f;
        }
        this.flags = tmpFlags;
    }

    public AppArtifact getArtifact() {
        return artifact;
    }

    public String getScope() {
        return scope;
    }

    public boolean isOptional() {
        return isFlagSet(OPTIONAL_FLAG);
    }

    public boolean isDirect() {
        return isFlagSet(DIRECT_FLAG);
    }

    public boolean isRuntimeExtensionArtifact() {
        return isFlagSet(RUNTIME_EXTENSION_ARTIFACT_FLAG);
    }

    public boolean isRuntimeCp() {
        return isFlagSet(RUNTIME_CP_FLAG);
    }

    public boolean isDeploymentCp() {
        return isFlagSet(DEPLOYMENT_CP_FLAG);
    }

    public boolean isFlagSet(int flag) {
        return (flags & flag) > 0;
    }

    public int getFlags() {
        return flags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact, flags, scope);
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
        return Objects.equals(artifact, other.artifact) && flags == other.flags && Objects.equals(scope, other.scope);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        artifact.append(buf).append('(');
        if (isDirect()) {
            buf.append("direct ");
        }
        if (isOptional()) {
            buf.append("optional ");
        }
        if (isRuntimeExtensionArtifact()) {
            buf.append("extension ");
        }
        if (isRuntimeCp()) {
            buf.append("runtime-cp ");
        }
        if (isDeploymentCp()) {
            buf.append("deployment-cp ");
        }
        return buf.append(scope).append(')').toString();
    }
}
