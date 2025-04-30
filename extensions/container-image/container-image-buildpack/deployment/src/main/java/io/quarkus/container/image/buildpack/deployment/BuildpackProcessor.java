package io.quarkus.container.image.buildpack.deployment;

import static io.quarkus.container.image.deployment.util.EnablementUtil.buildContainerImageNeeded;
import static io.quarkus.container.image.deployment.util.EnablementUtil.pushContainerImageNeeded;
import static io.quarkus.container.util.PathsUtil.findMainSourcesRoot;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.PushResponseItem;

import dev.snowdrop.buildpack.BuildConfig;
import dev.snowdrop.buildpack.BuildConfigBuilder;
import dev.snowdrop.buildpack.BuildConfigFluent.DockerConfigNested;
import dev.snowdrop.buildpack.config.ImageReference;
import dev.snowdrop.buildpack.config.RegistryAuthConfig;
import dev.snowdrop.buildpack.config.RegistryAuthConfigBuilder;
import dev.snowdrop.buildpack.docker.DockerClientUtils;
import dev.snowdrop.buildpack.docker.DockerClientUtils.HostAndSocket;
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
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveResultBuildItem;
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
            Optional<JvmStartupOptimizerArchiveResultBuildItem> jvmStartupOptimizerArchiveResult,
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

        Map<String, String> envMap = new HashMap<>(buildpackConfig.builderEnv());
        if (!envMap.isEmpty()) {
            log.info("Using builder environment with vars " + envMap.keySet());
        }

        List<RegistryAuthConfig> authConfigs = new ArrayList<>();

        //read in any configured per-registry auth info.
        Map<String, String> registryUserMap = new HashMap<>(buildpackConfig.registryUser());
        Map<String, String> registryPasswordMap = new HashMap<>(buildpackConfig.registryPassword());
        Map<String, String> registryTokenMap = new HashMap<>(buildpackConfig.registryToken());

        Set<String> registries = new HashSet<>();
        registries.addAll(registryUserMap.keySet());
        registries.addAll(registryPasswordMap.keySet());
        registries.addAll(registryTokenMap.keySet());

        //combine per-registry auth arguments into authConfig objects to use during the build.
        for (String registry : registries) {
            authConfigs.add(RegistryAuthConfig.builder().accept(RegistryAuthConfigBuilder.class, a -> {
                a.withRegistryAddress(registry);
                if (registryUserMap.containsKey(registry)) {
                    log.debug("adding username to auth credential for " + registry);
                    a.withUsername(registryUserMap.get(registry));
                }
                if (registryPasswordMap.containsKey(registry)) {
                    log.debug("adding password to auth credential for " + registry);
                    a.withPassword(registryPasswordMap.get(registry));
                }
                if (registryTokenMap.containsKey(registry)) {
                    log.debug("adding token to auth credential for " + registry);
                    a.withRegistryToken(registryTokenMap.get(registry));
                }
            }).build());
        }

        //add authConfig for container-image credential properties, if present.
        if (containerImageConfig.username().isPresent() &&
                containerImageConfig.password().isPresent()) {

            String registry = null;
            //user has supplied user&pwd, need to determine registry address.
            if (!containerImageConfig.registry().isPresent()) {
                //attempt to retrieve registry from image name
                registry = containerImage.getRegistry().orElseGet(() -> {
                    log.info("No container image registry was set, so 'docker.io' will be used");
                    return "docker.io";
                });
            } else {
                //use supplied registry address
                registry = containerImageConfig.registry().get();
            }

            if (registry != null &&
                    !registries.contains(registry)) {
                //add registry creds if set via quarkus main properties.
                //note buildpack specific creds take precedence if duplicated registry is present.
                log.debug("Adding auth creds from container-image properties");
                authConfigs.add(RegistryAuthConfig.builder()
                        .withUsername(containerImageConfig.username().get())
                        .withPassword(containerImageConfig.password().get())
                        .withRegistryAddress(registry)
                        .build());
            }
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
                    .withLogLevel(buildpackConfig.logLevel())
                    .withUseTimestamps(buildpackConfig.getUseTimestamps())
                    .endLogConfig()
                    .withNewDockerConfig()
                    .withPullRetryIncreaseSeconds(buildpackConfig.pullTimeoutIncreaseSeconds())
                    .withPullTimeoutSeconds(buildpackConfig.pullTimeoutSeconds())
                    .withPullRetryCount(buildpackConfig.pullRetryCount())
                    .withDockerNetwork(buildpackConfig.dockerNetwork().orElse(null))
                    .withUseDaemon(buildpackConfig.useDaemon())
                    .withAuthConfigs(authConfigs)
                    .endDockerConfig()
                    .accept(BuildConfigBuilder.class, b -> {

                        //swap to native image if present and if building native.
                        if (isNativeBuild) {
                            buildpackConfig.nativeBuilderImage().ifPresent(i -> b.withBuilderImage(new ImageReference(i)));
                        } else {
                            b.withBuilderImage(new ImageReference(buildpackConfig.jvmBuilderImage()));
                        }

                        //add run image if present
                        if (buildpackConfig.runImage().isPresent()) {
                            log.info("Using Run image of " + buildpackConfig.runImage().get());
                            b.withRunImage(new ImageReference(buildpackConfig.runImage().get()));
                        }

                        //ask for image to be trusted
                        if (buildpackConfig.trustBuilderImage().isPresent()) {
                            log.info("Setting trusted image to " + buildpackConfig.trustBuilderImage().get());
                            b.editPlatformConfig().withTrustBuilder(buildpackConfig.trustBuilderImage().get())
                                    .endPlatformConfig();
                        }

                        //configure dockerhost/socket if required
                        DockerConfigNested<BuildConfigBuilder> dc = b.editDockerConfig();
                        buildpackConfig.dockerHost().ifPresent(dh -> dc.withDockerHost(dh));
                        buildpackConfig.dockerSocket().ifPresent(ds -> dc.withDockerSocket(ds));
                        dc.endDockerConfig();

                        //configure lifecycle override image if present
                        buildpackConfig.lifecycleImage()
                                .ifPresent(
                                        li -> b.editPlatformConfig().withLifecycleImage(new ImageReference(li))
                                                .endPlatformConfig());

                        //force platform level, if present.
                        buildpackConfig.platformLevel()
                                .ifPresent(
                                        pl -> b.editPlatformConfig().withPlatformLevel(pl).endPlatformConfig());

                    })
                    .build()
                    .getExitCode();

            if (exitCode != 0) {
                throw new IllegalStateException("Buildpack build failed");
            }

            log.info("Buildpack build complete");
        }

        //if we built with registry mode, the build happened directly to the remote registry, so there is no need to publish
        //otherwise, now process the local image & publish to registry.
        if (pushContainerImage && Boolean.TRUE.equals(buildpackConfig.useDaemon())) {
            log.info("Pushing image to registry");
            Stream.concat(Stream.of(containerImage.getImage()), containerImage.getAdditionalImageTags().stream()).forEach(i -> {

                HostAndSocket hns = DockerClientUtils.probeContainerRuntime(
                        new HostAndSocket(buildpackConfig.dockerHost().orElse(""), buildpackConfig.dockerSocket().orElse("")));
                DockerClient dockerClient = DockerClientUtils.getDockerClient(hns, authConfigs);

                ResultCallback.Adapter<PushResponseItem> callback = new ResultCallback.Adapter<>() {
                    @Override
                    public void onNext(PushResponseItem object) {
                        log.info(object.toString());
                    }
                };
                dockerClient.pushImageCmd(i).exec(callback);
                try {
                    callback.awaitCompletion();
                } catch (Exception e) {
                    log.error(e);
                }

            });
        }
        return targetImageName;
    }

}
