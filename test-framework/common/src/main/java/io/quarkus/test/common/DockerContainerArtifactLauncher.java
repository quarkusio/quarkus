package io.quarkus.test.common;

import java.util.Map;

public interface DockerContainerArtifactLauncher extends ArtifactLauncher<DockerContainerArtifactLauncher.DockerInitContext> {

    interface DockerInitContext extends InitContext {

        String containerImage();

        boolean pullRequired();

        Map<Integer, Integer> additionalExposedPorts();
    }
}
