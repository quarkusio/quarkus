package io.quarkus.test.common;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Launches a packaged Quarkus container image for integration tests.
 */
public interface DockerContainerArtifactLauncher extends ArtifactLauncher<DockerContainerArtifactLauncher.DockerInitContext> {

    /**
     * Initialization values supplied to a container-image launcher.
     */
    interface DockerInitContext extends InitContext {

        /**
         * @return the container image to launch
         */
        String containerImage();

        /**
         * @return whether the launcher must pull the image before starting it
         */
        boolean pullRequired();

        /**
         * @return additional host-port to container-port mappings
         */
        Map<Integer, Integer> additionalExposedPorts();

        /**
         * @return labels to set on the integration-test container
         */
        Map<String, String> labels();

        /**
         * @return host-path to container-path volume mappings
         */
        Map<String, String> volumeMounts();

        /**
         * @return an entry point that replaces the image entry point, when configured
         */
        Optional<String> entryPoint();

        /**
         * @return the working directory reported for the image, when known
         */
        Optional<String> containerWorkingDirectory();

        /**
         * @return arguments appended after the image reference
         */
        List<String> programArgs();

        /**
         * @return whether the legacy AOT-only recording workflow is enabled
         */
        boolean generateAotFile();

        /**
         * @return additional JVM arguments for startup-archive recording
         */
        List<String> additionalRecordingArgs();

        /**
         * @return the host output directory used for legacy container-launch metadata and AOT output
         */
        String outputTargetDirectory();

        /**
         * Returns the explicit typed training request supplied by build-tool metadata.
         * <p>
         * The default is empty so existing custom launcher contexts retain their previous behavior.
         *
         * @return the explicit startup-archive training request, when present
         */
        default Optional<JvmStartupArchiveTraining> startupArchiveTraining() {
            return Optional.empty();
        }
    }
}
