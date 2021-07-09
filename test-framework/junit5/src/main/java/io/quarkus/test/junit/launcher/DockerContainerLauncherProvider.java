package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_WAIT_TIME_SECONDS;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.Config;

import io.quarkus.test.common.DefaultDockerContainerLauncher;
import io.quarkus.test.common.DockerContainerArtifactLauncher;
import io.quarkus.test.common.LauncherUtil;

public class DockerContainerLauncherProvider implements ArtifactLauncherProvider {

    @Override
    public boolean supportsArtifactType(String type) {
        return "jar-container".equals(type) || "native-container".equals(type);
    }

    @Override
    public DockerContainerArtifactLauncher create(CreateContext context) {
        String containerImage = context.quarkusArtifactProperties().getProperty("metadata.container-image");
        boolean pullRequired = Boolean
                .parseBoolean(context.quarkusArtifactProperties().getProperty("metadata.pull-required", "false"));
        if ((containerImage != null) && !containerImage.isEmpty()) {
            DockerContainerArtifactLauncher launcher;
            ServiceLoader<DockerContainerArtifactLauncher> loader = ServiceLoader.load(DockerContainerArtifactLauncher.class);
            Iterator<DockerContainerArtifactLauncher> iterator = loader.iterator();
            if (iterator.hasNext()) {
                launcher = iterator.next();
            } else {
                launcher = new DefaultDockerContainerLauncher();
            }
            Config config = LauncherUtil.installAndGetSomeConfig();
            launcher.init(new DefaultDockerInitContext(
                    config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                    config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                    Duration.ofSeconds(config.getValue("quarkus.test.jar-wait-time", OptionalLong.class)
                            .orElse(DEFAULT_WAIT_TIME_SECONDS)),
                    config.getOptionalValue("quarkus.test.native-image-profile", String.class).orElse(null),
                    ConfigUtil.argLineValue(config),
                    containerImage,
                    pullRequired));
            return launcher;
        } else {
            throw new IllegalStateException("The container image to be launched could not be determined");
        }
    }

    static class DefaultDockerInitContext extends DefaultInitContextBase
            implements DockerContainerArtifactLauncher.DockerInitContext {
        private final String containerImage;
        private final boolean pullRequired;

        public DefaultDockerInitContext(int httpPort, int httpsPort, Duration waitTime, String testProfile,
                List<String> argLine,
                String containerImage, boolean pullRequired) {
            super(httpPort, httpsPort, waitTime, testProfile, argLine);
            this.containerImage = containerImage;
            this.pullRequired = pullRequired;
        }

        @Override
        public String containerImage() {
            return containerImage;
        }

        @Override
        public boolean pullRequired() {
            return pullRequired;
        }
    }
}
