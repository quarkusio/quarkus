package io.quarkus.test.common;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DockerContainerArtifactLauncher extends ArtifactLauncher<DockerContainerArtifactLauncher.DockerInitContext> {

    interface DockerInitContext extends InitContext {

        String containerImage();

        boolean pullRequired();

        Map<Integer, Integer> additionalExposedPorts();

        Map<String, String> labels();

        Map<String, String> volumeMounts();

        Optional<String> entryPoint();

        Optional<String> containerWorkingDirectory();

        List<String> programArgs();

        boolean generateAotFile();

        String outputTargetDirectory();
    }
}
