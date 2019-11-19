package io.quarkus.platform.templates;

public class BomTemplate {

    private final ElementTemplate groupId;
    private final ElementTemplate artifactId;
    private final ElementTemplate version;

    public BomTemplate(ElementTemplate groupId, ElementTemplate artifactId, ElementTemplate version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public ElementTemplate getGroupId() {
        return groupId;
    }

    public ElementTemplate getArtifactId() {
        return artifactId;
    }

    public ElementTemplate getVersion() {
        return version;
    }
}
