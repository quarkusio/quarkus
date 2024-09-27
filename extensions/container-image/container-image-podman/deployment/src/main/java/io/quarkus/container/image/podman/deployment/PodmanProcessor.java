package io.quarkus.container.image.podman.deployment;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.docker.common.deployment.CommonProcessor;
import io.quarkus.container.spi.AvailableContainerImageExtensionBuildItem;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageBuilderBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.deployment.IsNormalNotRemoteDev;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.PodmanStatusBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.AppCDSResultBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.UpxCompressedBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;
import io.quarkus.deployment.util.ExecUtil;

public class PodmanProcessor extends CommonProcessor<PodmanConfig> {
    private static final Logger LOG = Logger.getLogger(PodmanProcessor.class);
    private static final String PODMAN = "podman";
    static final String PODMAN_CONTAINER_IMAGE_NAME = "podman";

    @Override
    protected String getProcessorImplementation() {
        return PODMAN;
    }

    @BuildStep
    public AvailableContainerImageExtensionBuildItem availability() {
        return new AvailableContainerImageExtensionBuildItem(PODMAN);
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, PodmanBuild.class }, onlyIfNot = NativeBuild.class)
    public void podmanBuildFromJar(PodmanConfig podmanConfig,
            PodmanStatusBuildItem podmanStatusBuildItem,
            ContainerImageConfig containerImageConfig,
            OutputTargetBuildItem out,
            ContainerImageInfoBuildItem containerImageInfo,
            @SuppressWarnings("unused") CompiledJavaVersionBuildItem compiledJavaVersion,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            @SuppressWarnings("unused") Optional<AppCDSResultBuildItem> appCDSResult, // ensure podman build will be performed after AppCDS creation
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder,
            PackageConfig packageConfig,
            @SuppressWarnings("unused") JarBuildItem jar) {

        buildFromJar(podmanConfig, podmanStatusBuildItem, containerImageConfig, out, containerImageInfo, buildRequest,
                pushRequest, artifactResultProducer, containerImageBuilder, packageConfig, ContainerRuntime.PODMAN);
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, NativeBuild.class, PodmanBuild.class })
    public void podmanBuildFromNativeImage(PodmanConfig podmanConfig,
            PodmanStatusBuildItem podmanStatusBuildItem,
            ContainerImageConfig containerImageConfig,
            ContainerImageInfoBuildItem containerImage,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            OutputTargetBuildItem out,
            @SuppressWarnings("unused") Optional<UpxCompressedBuildItem> upxCompressed, // used to ensure that we work with the compressed native binary if compression was enabled
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder,
            PackageConfig packageConfig,
            // used to ensure that the native binary has been built
            NativeImageBuildItem nativeImage) {

        buildFromNativeImage(podmanConfig, podmanStatusBuildItem, containerImageConfig, containerImage,
                buildRequest, pushRequest, out, artifactResultProducer, containerImageBuilder, packageConfig, nativeImage,
                ContainerRuntime.PODMAN);
    }

    @Override
    protected String createContainerImage(ContainerImageConfig containerImageConfig,
            PodmanConfig podmanConfig,
            ContainerImageInfoBuildItem containerImageInfo,
            OutputTargetBuildItem out,
            DockerfilePaths dockerfilePaths,
            boolean buildContainerImage,
            boolean pushContainerImage,
            PackageConfig packageConfig,
            String executableName) {

        // Following https://developers.redhat.com/articles/2023/11/03/how-build-multi-architecture-container-images#testing_multi_architecture_containers
        // If we are building more than 1 platform, then the build needs to happen in 2 separate steps
        // 1) podman manifest create <image_name>
        // 2) podman build --platform <platforms> --manifest <image_name>

        // Then when pushing you push the manifest, not the image:
        // podman manifest push <image_name>

        var isMultiPlatformBuild = isMultiPlatformBuild(podmanConfig);
        var image = containerImageInfo.getImage();

        if (isMultiPlatformBuild) {
            createManifest(image, executableName);
        }

        if (buildContainerImage) {
            var podmanBuildArgs = getPodmanBuildArgs(image, dockerfilePaths, containerImageConfig, podmanConfig,
                    isMultiPlatformBuild);
            buildImage(containerImageInfo, out, executableName, podmanBuildArgs, true);
        }

        if (pushContainerImage) {
            loginToRegistryIfNeeded(containerImageConfig, containerImageInfo, executableName);

            if (isMultiPlatformBuild) {
                pushManifests(containerImageInfo, executableName);
            } else {
                pushImages(containerImageInfo, executableName, podmanConfig);
            }
        }

        return image;
    }

    @Override
    protected String[] createPushArgs(String image, PodmanConfig config) {
        return new String[] { "push", image, String.format("--tls-verify=%b", config.tlsVerify()) };
    }

    private String[] getPodmanBuildArgs(String image,
            DockerfilePaths dockerfilePaths,
            ContainerImageConfig containerImageConfig,
            PodmanConfig podmanConfig,
            boolean isMultiPlatformBuild) {

        var podmanBuildArgs = getContainerCommonBuildArgs(image, dockerfilePaths, containerImageConfig, podmanConfig,
                !isMultiPlatformBuild);

        podmanConfig.platform()
                .filter(platform -> !platform.isEmpty())
                .ifPresent(platform -> {
                    podmanBuildArgs.addAll(List.of("--platform", String.join(",", platform)));

                    if (isMultiPlatformBuild) {
                        podmanBuildArgs.addAll(List.of("--manifest", image));
                    }
                });

        podmanBuildArgs.add(dockerfilePaths.dockerExecutionPath().toAbsolutePath().toString());
        return podmanBuildArgs.toArray(String[]::new);
    }

    private void pushManifests(ContainerImageInfoBuildItem containerImageInfo, String executableName) {
        Stream.concat(containerImageInfo.getAdditionalImageTags().stream(), Stream.of(containerImageInfo.getImage()))
                .forEach(manifestToPush -> pushManifest(manifestToPush, executableName));
    }

    private void pushManifest(String image, String executableName) {
        String[] pushArgs = { "manifest", "push", image };
        var pushSuccessful = ExecUtil.exec(executableName, pushArgs);

        if (!pushSuccessful) {
            throw containerRuntimeException(executableName, pushArgs);
        }

        LOG.infof("Successfully pushed podman manifest %s", image);
    }

    private void createManifest(String image, String executableName) {
        var manifestCreateArgs = new String[] { "manifest", "create", image };

        LOG.infof("Running '%s %s'", executableName, String.join(" ", manifestCreateArgs));
        var createManifestSuccessful = ExecUtil.exec(executableName, manifestCreateArgs);

        if (!createManifestSuccessful) {
            throw containerRuntimeException(executableName, manifestCreateArgs);
        }
    }

    private boolean isMultiPlatformBuild(PodmanConfig podmanConfig) {
        return podmanConfig.platform()
                .map(List::size)
                .orElse(0) >= 2;
    }
}
