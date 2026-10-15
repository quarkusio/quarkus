package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.DefaultKubernetesContainerLauncher;
import io.quarkus.test.common.KubernetesContainerArtifactLauncher;
import io.quarkus.test.common.TestConfigUtil;
import io.smallrye.config.Config;

public class KubernetesContainerLauncherProvider implements ArtifactLauncherProvider {

    /**
     * Unlike the jar/native/container artifact types, there's no build-time signal that ever sets the artifact
     * type to "kubernetes"/"openshift" - this launch mode is instead selected purely by
     * {@code quarkus.test.kubernetes.registry} or {@code quarkus.test.openshift.registry} being configured, since
     * those properties have no other purpose and are never set by accident, regardless of the artifact type that
     * {@code QuarkusIntegrationTest} resolved.
     */
    @Override
    public boolean supportsArtifactType(String type, String testProfile) {
        Config config = Config.get();
        return config.getOptionalValue("quarkus.test.kubernetes.registry", String.class).isPresent()
                || config.getOptionalValue("quarkus.test.openshift.registry", String.class).isPresent();
    }

    @Override
    public KubernetesContainerArtifactLauncher create(CreateContext context) {
        String containerImage = context.quarkusArtifactProperties().getProperty("metadata.container-image");
        if ((containerImage == null) || containerImage.isEmpty()) {
            throw new IllegalStateException(
                    "No container image was found for this build. Make sure 'quarkus.container-image.build=true' "
                            + "(with a docker/jib/podman builder) is set when building the application under test.");
        }

        Config config = Config.get();
        TestConfig testConfig = config.getConfigMapping(TestConfig.class);

        // Which one of these is set determines the target - deliberately not guessed any other way (e.g. from
        // which manifest(s) happen to exist on disk), since a project can easily have both a kubernetes.yml and an
        // openshift.yml generated at once.
        Optional<String> kubernetesRegistry = config.getOptionalValue("quarkus.test.kubernetes.registry", String.class);
        Optional<String> openshiftRegistry = config.getOptionalValue("quarkus.test.openshift.registry", String.class);

        String deploymentTarget;
        String testRegistry;
        if (kubernetesRegistry.isPresent() && openshiftRegistry.isPresent()) {
            throw new IllegalStateException(
                    "Both 'quarkus.test.kubernetes.registry' and 'quarkus.test.openshift.registry' are set; only "
                            + "one should be configured, since whichever one is set determines whether "
                            + "@QuarkusIntegrationTest deploys to 'kubernetes' or 'openshift'.");
        } else if (kubernetesRegistry.isPresent()) {
            deploymentTarget = "kubernetes";
            testRegistry = kubernetesRegistry.get();
        } else if (openshiftRegistry.isPresent()) {
            deploymentTarget = "openshift";
            testRegistry = openshiftRegistry.get();
        } else {
            throw new IllegalStateException(
                    "Either 'quarkus.test.kubernetes.registry' or 'quarkus.test.openshift.registry' must be set to "
                            + "the registry (reachable from the cluster) to push the test image to");
        }

        Path manifestPath = context.buildOutputDirectory().resolve("kubernetes").resolve(deploymentTarget + ".yml");
        if (!Files.exists(manifestPath)) {
            throw new IllegalStateException(
                    "'quarkus.test." + deploymentTarget + ".registry' is set, but no '" + deploymentTarget
                            + ".yml' manifest was found at '" + manifestPath.toAbsolutePath() + "'. Make sure the '"
                            + ("kubernetes".equals(deploymentTarget) ? "quarkus-kubernetes" : "quarkus-openshift")
                            + "' extension is present.");
        }

        // Every other quarkus.test.<target>.* property below is resolved under the SAME target that
        // quarkus.test.<target>.registry matched - kubernetes and openshift settings are fully independent of
        // each other, mirroring how KubernetesConfig/OpenShiftConfig never share values despite having the same
        // shape.
        String targetPrefix = "quarkus.test." + deploymentTarget + ".";

        // If not set explicitly, auto-detect: external only makes sense if the manifest actually has a
        // Route/Ingress to find, which requires expose=true to have been set at the application level - not
        // every app that opts into quarkus.test.<target>.registry will have done that.
        TestConfig.ClusterTestTarget.Exposure exposure = config
                .getOptionalValue(targetPrefix + "exposure", TestConfig.ClusterTestTarget.Exposure.class)
                .orElseGet(() -> hasRouteOrIngress(manifestPath, deploymentTarget)
                        ? TestConfig.ClusterTestTarget.Exposure.EXTERNAL
                        : TestConfig.ClusterTestTarget.Exposure.PORT_FORWARD);

        DefaultKubernetesContainerLauncher launcher = new DefaultKubernetesContainerLauncher();
        launcher.init(new DefaultKubernetesInitContext(
                config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                testConfig.waitTime(),
                config.getOptionalValue("quarkus.shutdown.timeout", Duration.class).orElse(Duration.ZERO),
                testConfig.integrationTestProfile(),
                TestConfigUtil.argLineValues(testConfig.argLine().orElse("")),
                testConfig.env(),
                context.devServicesLaunchResult(),
                manifestPath,
                deploymentTarget,
                containerImage,
                testRegistry,
                config.getOptionalValue(targetPrefix + "tag", String.class),
                config.getOptionalValue(targetPrefix + "insecure-registry", Boolean.class).orElse(false),
                config.getOptionalValue(targetPrefix + "namespace", String.class),
                exposure.toString(),
                config.getValue(targetPrefix + "external-port", OptionalInt.class),
                config.getOptionalValue(targetPrefix + "delete-after-test", Boolean.class).orElse(true),
                config.getOptionalValue(targetPrefix + "wait-timeout", Duration.class).orElse(Duration.ofMinutes(2))));
        return launcher;
    }

    private static boolean hasRouteOrIngress(Path manifestPath, String deploymentTarget) {
        String expectedKind = "openshift".equals(deploymentTarget) ? "Route" : "Ingress";
        try {
            return Files.readAllLines(manifestPath).stream()
                    .anyMatch(line -> line.strip().equals("kind: " + expectedKind));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read manifest at '" + manifestPath.toAbsolutePath() + "'", e);
        }
    }

    static class DefaultKubernetesInitContext extends DefaultInitContextBase
            implements KubernetesContainerArtifactLauncher.KubernetesInitContext {
        private final Path manifestPath;
        private final String deploymentTarget;
        private final String containerImage;
        private final String testRegistry;
        private final Optional<String> testTag;
        private final boolean insecureRegistry;
        private final Optional<String> namespace;
        private final String exposure;
        private final OptionalInt externalPort;
        private final boolean deleteAfterTest;
        private final Duration waitTimeout;

        public DefaultKubernetesInitContext(int httpPort, int httpsPort, Duration waitTime, Duration shutdownTimeout,
                String testProfile, List<String> argLine, Map<String, String> env,
                ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult,
                Path manifestPath, String deploymentTarget, String containerImage, String testRegistry,
                Optional<String> testTag, boolean insecureRegistry, Optional<String> namespace, String exposure,
                OptionalInt externalPort, boolean deleteAfterTest, Duration waitTimeout) {
            super(httpPort, httpsPort, waitTime, shutdownTimeout, testProfile, argLine, env, devServicesLaunchResult);
            this.manifestPath = manifestPath;
            this.deploymentTarget = deploymentTarget;
            this.containerImage = containerImage;
            this.testRegistry = testRegistry;
            this.testTag = testTag;
            this.insecureRegistry = insecureRegistry;
            this.namespace = namespace;
            this.exposure = exposure;
            this.externalPort = externalPort;
            this.deleteAfterTest = deleteAfterTest;
            this.waitTimeout = waitTimeout;
        }

        @Override
        public Path manifestPath() {
            return manifestPath;
        }

        @Override
        public String deploymentTarget() {
            return deploymentTarget;
        }

        @Override
        public String containerImage() {
            return containerImage;
        }

        @Override
        public String testRegistry() {
            return testRegistry;
        }

        @Override
        public Optional<String> testTag() {
            return testTag;
        }

        @Override
        public boolean insecureRegistry() {
            return insecureRegistry;
        }

        @Override
        public Optional<String> namespace() {
            return namespace;
        }

        @Override
        public String exposure() {
            return exposure;
        }

        @Override
        public OptionalInt externalPort() {
            return externalPort;
        }

        @Override
        public boolean deleteAfterTest() {
            return deleteAfterTest;
        }

        @Override
        public Duration waitTimeout() {
            return waitTimeout;
        }
    }
}
