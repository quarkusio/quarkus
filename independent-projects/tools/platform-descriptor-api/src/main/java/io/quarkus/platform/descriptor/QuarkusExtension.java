package io.quarkus.platform.descriptor;

import io.quarkus.dependencies.Extension;

@SuppressWarnings("deprecation")
public class QuarkusExtension extends Extension {

    public QuarkusExtension() {
        // Used by mapper.
        super();
    }

    public QuarkusExtension(String groupId, String artifactId, String version) {
        super(groupId, artifactId, version);
    }
}
