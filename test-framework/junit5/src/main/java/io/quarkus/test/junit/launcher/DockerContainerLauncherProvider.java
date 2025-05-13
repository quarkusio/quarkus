package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.ArtifactTypeUtil.isContainer;
import static io.quarkus.test.junit.ArtifactTypeUtil.isJar;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.deployment.images.ContainerImages;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.DefaultDockerContainerLauncher;
import io.quarkus.test.common.DockerContainerArtifactLauncher;
import io.quarkus.test.common.TestConfigUtil;
import io.smallrye.config.SmallRyeConfig;

public class DockerContainerLauncherProvider implements ArtifactLauncherProvider {

    @Override
    public boolean supportsArtifactType(String type, String testProfile) {
        return isContainer(type) || (isJar(type) && "test-with-native-agent".equals(testProfile));
    }

    @Override
    public DockerContainerArtifactLauncher create(CreateContext context) {
        String containerImage = context.quarkusArtifactProperties().getProperty("metadata.container-image");
        boolean pullRequired = Boolean
                .parseBoolean(context.quarkusArtifactProperties().getProperty("metadata.pull-required", "false"));
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        if ((containerImage != null) && !containerImage.isEmpty()) {
            DockerContainerArtifactLauncher launcher;
            ServiceLoader<DockerContainerArtifactLauncher> loader = ServiceLoader.load(DockerContainerArtifactLauncher.class);
            Iterator<DockerContainerArtifactLauncher> iterator = loader.iterator();
            if (iterator.hasNext()) {
                launcher = iterator.next();
            } else {
                launcher = new DefaultDockerContainerLauncher();
            }
            launcherInit(context, launcher, config, containerImage, pullRequired, Optional.empty(), volumeMounts(config),
                    Collections.emptyList());
            return launcher;
        } else {
            // Running quarkus integration tests with a native image agent,
            // which can be achieved with a specific test profile name,
            // involves having Quarkus run with the java process inside the default Mandrel builder container image.
            // This block achieves this by swapping the entry point to be the java executable,
            // adding a volume mapping pointing to the build output directory,
            // and then instructing the java process to run the run jar,
            // along with the native image agent arguments and any other additional parameters.
            TestConfig testConfig = config.getConfigMapping(TestConfig.class);
            if ("test-with-native-agent".equals(testConfig.integrationTestProfile())) {
                DockerContainerArtifactLauncher launcher = new DefaultDockerContainerLauncher();
                Optional<String> entryPoint = Optional.of("java");
                Map<String, String> volumeMounts = new HashMap<>(volumeMounts(config));
                volumeMounts.put(context.buildOutputDirectory().toString(), "/project");
                containerImage = ContainerImages.UBI9_MANDREL_BUILDER;

                List<String> programArgs = new ArrayList<>();
                addNativeAgentProgramArgs(programArgs, context);

                launcherInit(context, launcher, config, containerImage, pullRequired, entryPoint, volumeMounts, programArgs);
                return launcher;
            } else {
                throw new IllegalStateException("The container image to be launched could not be determined");
            }
        }
    }

    private void launcherInit(CreateContext context, DockerContainerArtifactLauncher launcher, SmallRyeConfig config,
            String containerImage, boolean pullRequired, Optional<String> entryPoint, Map<String, String> volumeMounts,
            List<String> programArgs) {
        TestConfig testConfig = config.getConfigMapping(TestConfig.class);
        launcher.init(new DefaultDockerInitContext(
                config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                testConfig.waitTime(),
                testConfig.integrationTestProfile(),
                TestConfigUtil.argLineValues(testConfig.argLine().orElse("")),
                testConfig.env(),
                context.devServicesLaunchResult(),
                containerImage,
                pullRequired,
                additionalExposedPorts(config),
                labels(config),
                volumeMounts,
                entryPoint,
                programArgs));
    }

    private void addNativeAgentProgramArgs(List<String> programArgs, CreateContext context) {
        final String outputPropertyName = System.getProperty("quarkus.test.native.agent.output.property.name",
                "config-output-dir");
        final String outputPropertyValue = System.getProperty("quarkus.test.native.agent.output.property.value",
                "native-image-agent-base-config");
        final String agentAdditionalArgs = System.getProperty("quarkus.test.native.agent.additional.args", "");

        final String accessFilter = "access-filter-file=quarkus-access-filter.json";
        final String callerFilter = "caller-filter-file=quarkus-caller-filter.json";

        final String output = String.format(
                "%s=%s", outputPropertyName, outputPropertyValue);

        String agentLibArg = String.format(
                "-agentlib:native-image-agent=%s,%s,%s,%s", accessFilter, callerFilter, output, agentAdditionalArgs);

        programArgs.add(agentLibArg);

        programArgs.add("-jar");
        final String jarPath = FileUtil.translateToVolumePath(context.quarkusArtifactProperties().getProperty("path"));
        programArgs.add(jarPath);
    }

    private Map<Integer, Integer> additionalExposedPorts(SmallRyeConfig config) {
        try {
            return config.getValues("quarkus.test.container.additional-exposed-ports", Integer.class, Integer.class);
        } catch (NoSuchElementException e) {
            return Collections.emptyMap();
        }
    }

    private Map<String, String> volumeMounts(SmallRyeConfig config) {
        try {
            return config.getValues("quarkus.test.container.volume-mounts", String.class, String.class);
        } catch (NoSuchElementException e) {
            return Collections.emptyMap();
        }
    }

    private Map<String, String> labels(SmallRyeConfig config) {
        try {
            return config.getValues("quarkus.test.container.labels", String.class, String.class);
        } catch (NoSuchElementException e) {
            return Collections.emptyMap();
        }
    }

    static class DefaultDockerInitContext extends DefaultInitContextBase
            implements DockerContainerArtifactLauncher.DockerInitContext {
        private final String containerImage;
        private final boolean pullRequired;
        private final Map<Integer, Integer> additionalExposedPorts;
        private final Optional<String> entryPoint;
        private final List<String> programArgs;
        private final Map<String, String> labels;
        private final Map<String, String> volumeMounts;

        public DefaultDockerInitContext(int httpPort, int httpsPort, Duration waitTime, String testProfile,
                List<String> argLine, Map<String, String> env,
                ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult,
                String containerImage, boolean pullRequired,
                Map<Integer, Integer> additionalExposedPorts,
                Map<String, String> labels,
                Map<String, String> volumeMounts, Optional<String> entryPoint, List<String> programArgs) {
            super(httpPort, httpsPort, waitTime, testProfile, argLine, env, devServicesLaunchResult);
            this.containerImage = containerImage;
            this.pullRequired = pullRequired;
            this.additionalExposedPorts = additionalExposedPorts;
            this.labels = labels;
            this.volumeMounts = volumeMounts;
            this.entryPoint = entryPoint;
            this.programArgs = programArgs;
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

        @Override
        public Map<String, String> labels() {
            return labels;
        }

        @Override
        public Map<String, String> volumeMounts() {
            return volumeMounts;
        }

        @Override
        public Optional<String> entryPoint() {
            return entryPoint;
        }

        @Override
        public List<String> programArgs() {
            return programArgs;
        }
    }
}
