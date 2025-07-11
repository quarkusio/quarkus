package io.quarkus.devui.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.GACT;

public final class DevUIWebJarBuildItem extends MultiBuildItem {
    private final GACT artifactKey;
    private final String path;

    public DevUIWebJarBuildItem(GACT artifactKey, String path) {
        this.artifactKey = artifactKey;
        this.path = path;
    }

    public GACT getArtifactKey() {
        return artifactKey;
    }

    public String getPath() {
        return path;
    }

}
