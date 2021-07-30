package io.quarkus.test.common;

public interface DockerContainerArtifactLauncher extends ArtifactLauncher<DockerContainerArtifactLauncher.DockerInitContext> {

    interface DockerInitContext extends InitContext {

        String containerImage();

        boolean pullRequired();
    }
}
