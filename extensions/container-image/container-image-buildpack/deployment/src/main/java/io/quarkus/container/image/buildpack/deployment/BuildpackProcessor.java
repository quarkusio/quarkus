package io.quarkus.container.image.buildpack.deployment;

import static io.quarkus.container.image.deployment.util.EnablementUtil.buildContainerImageNeeded;
import static io.quarkus.container.image.deployment.util.EnablementUtil.pushContainerImageNeeded;
import static io.quarkus.container.util.PathsUtil.findMainSourcesRoot;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.PushResponseItem;

import dev.snowdrop.buildpack.BuildConfig;
import dev.snowdrop.buildpack.BuildConfigBuilder;
import dev.snowdrop.buildpack.config.ImageReference;
import dev.snowdrop.buildpack.docker.DockerClientUtils;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.util.NativeBinaryUtil;
import io.quarkus.container.spi.AvailableContainerImageExtensionBuildItem;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageBuilderBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.deployment.IsNormalNotRemoteDev;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.pkg.builditem.AppCDSResultBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;

public class BuildpackProcessor {

    private static final Logger log = Logger.getLogger(BuildpackProcessor.class);
    private static final String QUARKUS_CONTAINER_IMAGE_BUILD = "QUARKUS_CONTAINER_IMAGE_BUILD";
    private static final String QUARKUS_CONTAINER_IMAGE_PUSH = "QUARKUS_CONTAINER_IMAGE_PUSH";

    public static final String BUILDPACK = "buildpack";

    private enum ProjectDirs {
        TARGET,
        SOURCE,
        ROOT
    };

    @BuildStep
    public AvailableContainerImageExtensionBuildItem availability() {
        return new AvailableContainerImageExtensionBuildItem(BUILDPACK);
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, BuildpackBuild.class }, onlyIfNot = NativeBuild.class)
    public void buildFromJar(ContainerImageConfig containerImageConfig, BuildpackConfig buildpackConfig,
            ContainerImageInfoBuildItem containerImage,
            JarBuildItem sourceJar,
            MainClassBuildItem mainClass,
            OutputTargetBuildItem outputTarget,
            CurateOutcomeBuildItem curateOutcome,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            List<ContainerImageLabelBuildItem> containerImageLabels,
            Optional<AppCDSResultBuildItem> appCDSResult,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder) {

        boolean buildContainerImage = buildContainerImageNeeded(containerImageConfig, buildRequest);
        boolean pushContainerImage = pushContainerImageNeeded(containerImageConfig, pushRequest);

        if (!buildContainerImage && !pushContainerImage) {
            return;
        }

        if (containerImageConfig.isBuildExplicitlyDisabled()) {
            return;
        }

        log.info("Starting (local) container image build for jar using buildpack.");
        String targetImageName = runBuildpackBuild(buildpackConfig, containerImage, containerImageConfig, buildContainerImage,
                pushContainerImage,
                outputTarget, false /* isNative */);

        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container",
                Collections.singletonMap("container-image", targetImageName)));
        containerImageBuilder.produce(new ContainerImageBuilderBuildItem(BUILDPACK));
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, BuildpackBuild.class, NativeBuild.class })
    public void buildFromNative(ContainerImageConfig containerImageConfig, BuildpackConfig buildpackConfig,
            ContainerImageInfoBuildItem containerImage,
            NativeImageBuildItem nativeImage,
            OutputTargetBuildItem outputTarget,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            List<ContainerImageLabelBuildItem> containerImageLabels,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder) {

        boolean buildContainerImage = buildContainerImageNeeded(containerImageConfig, buildRequest);
        boolean pushContainerImage = pushContainerImageNeeded(containerImageConfig, pushRequest);

        if (!buildContainerImage && !pushContainerImage) {
            return;
        }

        if (containerImageConfig.isBuildExplicitlyDisabled()) {
            return;
        }

        if (!NativeBinaryUtil.nativeIsLinuxBinary(nativeImage)) {
            throw new RuntimeException(
                    "The native binary produced by the build is not a Linux binary and therefore cannot be used in a Linux container image. Consider adding \"quarkus.native.container-build=true\" to your configuration");
        }

        log.info("Starting (local) container image build for native binary using buildpack.");
        String targetImageName = runBuildpackBuild(buildpackConfig, containerImage, containerImageConfig, buildContainerImage,
                pushContainerImage,
                outputTarget, true /* isNative */);

        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container",
                Collections.singletonMap("container-image", targetImageName)));
        containerImageBuilder.produce(new ContainerImageBuilderBuildItem(BUILDPACK));
    }

    private Map<ProjectDirs, Path> getPaths(OutputTargetBuildItem outputTarget) {

        Path targetDirectory = outputTarget.getOutputDirectory();

        Map.Entry<Path, Path> mainSourcesRoot = findMainSourcesRoot(targetDirectory);
        if (mainSourcesRoot == null) {
            throw new RuntimeException("Buildpack build unable to determine project dir");
        }
        Path sourceRoot = mainSourcesRoot.getKey();
        Path projectRoot = mainSourcesRoot.getValue();
        if (!projectRoot.toFile().exists() || !sourceRoot.toFile().exists()) {
            throw new RuntimeException("Buildpack build unable to verify project dir");
        }

        Map<ProjectDirs, Path> result = new HashMap<>();
        result.put(ProjectDirs.ROOT, projectRoot);
        result.put(ProjectDirs.SOURCE, sourceRoot);
        result.put(ProjectDirs.TARGET, targetDirectory);
        return result;
    }

    private String runBuildpackBuild(BuildpackConfig buildpackConfig,
            ContainerImageInfoBuildItem containerImage,
            ContainerImageConfig containerImageConfig,
            boolean buildContainerImage,
            boolean pushContainerImage,
            OutputTargetBuildItem outputTarget,
            boolean isNativeBuild) {

        Map<ProjectDirs, Path> dirs = getPaths(outputTarget);

        log.debug("Using target dir of " + dirs.get(ProjectDirs.TARGET));
        log.debug("Using source root of " + dirs.get(ProjectDirs.SOURCE));
        log.debug("Using project root of " + dirs.get(ProjectDirs.ROOT));

        String targetImageName = containerImage.getImage();
        log.debug("Using Destination image of " + targetImageName);

        Map<String, String> envMap = new HashMap<>(buildpackConfig.builderEnv);
        if (!envMap.isEmpty()) {
            log.info("Using builder environment of " + envMap);
        }

        // Let's explicitly disable build and push during the build to avoid inception style builds
        envMap.put(QUARKUS_CONTAINER_IMAGE_BUILD, "false");
        envMap.put(QUARKUS_CONTAINER_IMAGE_PUSH, "false");

        if (buildContainerImage) {
            log.info("Initiating Buildpack build");
            int exitCode = BuildConfig.builder()
                    .addNewFileContentApplication(dirs.get(ProjectDirs.ROOT).toFile())
                    .withOutputImage(new ImageReference(targetImageName))
                    .withNewPlatformConfig()
                    .withEnvironment(envMap)
                    .endPlatformConfig()
                    .withNewLogConfig()
                    .withLogger(new BuildpackLogger())
                    .withLogLevel(buildpackConfig.logLevel)
                    .endLogConfig()
                    .withNewDockerConfig()
                    .withPullRetryIncreaseSeconds(buildpackConfig.pullTimeoutIncreaseSeconds)
                    .withPullTimeoutSeconds(buildpackConfig.pullTimeoutSeconds)
                    .withPullRetryCount(buildpackConfig.pullRetryCount)
                    .endDockerConfig()
                    .accept(BuildConfigBuilder.class, b -> {
                        if (isNativeBuild) {
                            buildpackConfig.nativeBuilderImage.ifPresent(i -> b.withBuilderImage(new ImageReference(i)));
                        } else {
                            b.withBuilderImage(new ImageReference(buildpackConfig.jvmBuilderImage));
                        }

                        if (buildpackConfig.runImage.isPresent()) {
                            log.info("Using Run image of " + buildpackConfig.runImage.get());
                            b.withRunImage(new ImageReference(buildpackConfig.runImage.get()));
                        }

                        if (buildpackConfig.dockerHost.isPresent()) {
                            log.info("Using DockerHost of " + buildpackConfig.dockerHost.get());
                            b.editDockerConfig().withDockerHost(buildpackConfig.dockerHost.get())
                                    .endDockerConfig();
                        }

                        if (buildpackConfig.trustBuilderImage.isPresent()) {
                            log.info("Setting trusted image to " + buildpackConfig.trustBuilderImage.get());
                            b.editPlatformConfig().withTrustBuilder(buildpackConfig.trustBuilderImage.get())
                                    .endPlatformConfig();
                        }
                    })
                    .build()
                    .getExitCode();

            if (exitCode != 0) {
                throw new IllegalStateException("Buildpack build failed");
            }

            log.info("Buildpack build complete");
        }

        if (pushContainerImage) {
            var registry = containerImage.getRegistry()
                    .orElseGet(() -> {
                        log.info("No container image registry was set, so 'docker.io' will be used");
                        return "docker.io";
                    });
            AuthConfig authConfig = new AuthConfig();
            authConfig.withRegistryAddress(registry);
            containerImageConfig.username.ifPresent(u -> authConfig.withUsername(u));
            containerImageConfig.password.ifPresent(p -> authConfig.withPassword(p));

            log.info("Pushing image to " + authConfig.getRegistryAddress());
            Stream.concat(Stream.of(containerImage.getImage()), containerImage.getAdditionalImageTags().stream()).forEach(i -> {
                //If no dockerHost is specified use empty String. The util will take care of the rest.
                String dockerHost = buildpackConfig.dockerHost.orElse("");
                ResultCallback.Adapter<PushResponseItem> callback = DockerClientUtils.getDockerClient(dockerHost)
                        .pushImageCmd(i).start();
                try {
                    callback.awaitCompletion();
                    log.info("Push complete");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        return targetImageName;
    }
}
