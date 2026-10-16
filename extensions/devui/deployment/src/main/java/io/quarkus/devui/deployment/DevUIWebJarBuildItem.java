package io.quarkus.devui.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.GACT;

public final class DevUIWebJarBuildItem extends MultiBuildItem {
    private final GACT artifactKey;
    private final String path;
    private final String namespace;

    public DevUIWebJarBuildItem(GACT artifactKey, String path, String namespace) {
        this.artifactKey = artifactKey;
        this.path = path;
        this.namespace = namespace;
    }

    /**
     * @return the Dev UI namespace of the extension, the artifactId of its runtime artifact (empty for Dev UI itself)
     */
    public String getNamespace() {
        return namespace;
    }

    public GACT getArtifactKey() {
        return artifactKey;
    }

    public String getPath() {
        return path;
    }

}
