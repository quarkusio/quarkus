package io.quarkus.maven.dependency;

abstract class AbstractDependencyBuilder<B extends AbstractDependencyBuilder<B, T>, T> {

    String groupId;
    String artifactId;
    String classifier = "";
    String type = GACTV.TYPE_JAR;
    String version;
    String scope = "compile";
    int flags;

    @SuppressWarnings("unchecked")
    public B setCoords(ArtifactCoords coords) {
        this.groupId = coords.getGroupId();
        this.artifactId = coords.getArtifactId();
        this.classifier = coords.getClassifier();
        this.type = coords.getType();
        this.version = coords.getVersion();
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setGroupId(String groupId) {
        this.groupId = groupId;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setClassifier(String classifier) {
        this.classifier = classifier;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setType(String type) {
        this.type = type;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setVersion(String version) {
        this.version = version;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setScope(String scope) {
        this.scope = scope;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setOptional(boolean optional) {
        if (optional) {
            setFlags(DependencyFlags.OPTIONAL);
        } else {
            clearFlag(DependencyFlags.OPTIONAL);
        }
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setDeploymentCp() {
        setFlags(DependencyFlags.DEPLOYMENT_CP);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setRuntimeCp() {
        setFlags(DependencyFlags.RUNTIME_CP);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setWorkspaceModule() {
        setFlags(DependencyFlags.WORKSPACE_MODULE);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setDirect(boolean direct) {
        if (direct) {
            setFlags(DependencyFlags.DIRECT);
        }
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setReloadable() {
        setFlags(DependencyFlags.RELOADABLE);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setRuntimeExtensionArtifact() {
        setFlags(DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setFlags(int flag) {
        this.flags |= flag;
        return (B) this;
    }

    public int getFlags() {
        return flags;
    }

    public boolean isFlagSet(int flag) {
        return (flags & flag) > 0;
    }

    public void clearFlag(int flag) {
        if ((flags & flag) > 0) {
            flags ^= flag;
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getScope() {
        return scope;
    }

    public String toGACTVString() {
        return getGroupId() + ":" + getArtifactId() + ":" + getClassifier() + ":" + getType() + ":" + getVersion();
    }

    public ArtifactKey getKey() {
        return new GACT(groupId, artifactId, classifier, type);
    }

    public abstract T build();
}
