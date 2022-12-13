package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.ArtifactTypeUtil.isContainer;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.ServiceLoader;

import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.DefaultDockerContainerLauncher;
import io.quarkus.test.common.DockerContainerArtifactLauncher;
import io.quarkus.test.common.LauncherUtil;
import io.smallrye.config.SmallRyeConfig;

public class DockerContainerLauncherProvider implements ArtifactLauncherProvider {

    @Override
    public boolean supportsArtifactType(String type) {
        return isContainer(type);
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
            SmallRyeConfig config = (SmallRyeConfig) LauncherUtil.installAndGetSomeConfig();
            launcher.init(new DefaultDockerInitContext(
                    config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                    config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                    ConfigUtil.waitTimeValue(config),
                    ConfigUtil.integrationTestProfile(config),
                    ConfigUtil.argLineValue(config),
                    context.devServicesLaunchResult(),
                    containerImage,
                    pullRequired,
                    additionalExposedPorts(config)));
            return launcher;
        } else {
            throw new IllegalStateException("The container image to be launched could not be determined");
        }
    }

    private Map<Integer, Integer> additionalExposedPorts(SmallRyeConfig config) {
        try {
            return config.getValues("quarkus.test.container.additional-exposed-ports", Integer.class, Integer.class);
        } catch (NoSuchElementException e) {
            return Collections.emptyMap();
        }
    }

    static class DefaultDockerInitContext extends DefaultInitContextBase
            implements DockerContainerArtifactLauncher.DockerInitContext {
        private final String containerImage;
        private final boolean pullRequired;
        private final Map<Integer, Integer> additionalExposedPorts;

        public DefaultDockerInitContext(int httpPort, int httpsPort, Duration waitTime, String testProfile,
                List<String> argLine, ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult,
                String containerImage, boolean pullRequired, Map<Integer, Integer> additionalExposedPorts) {
            super(httpPort, httpsPort, waitTime, testProfile, argLine, devServicesLaunchResult);
            this.containerImage = containerImage;
            this.pullRequired = pullRequired;
            this.additionalExposedPorts = additionalExposedPorts;
        }

        @Override
        public String containerImage() {
            return containerImage;
        }

        @Override
        public boolean pullRequired() {
            return pullRequired;
        }

        @Override
        public Map<Integer, Integer> additionalExposedPorts() {
            return additionalExposedPorts;
        }
    }
}
