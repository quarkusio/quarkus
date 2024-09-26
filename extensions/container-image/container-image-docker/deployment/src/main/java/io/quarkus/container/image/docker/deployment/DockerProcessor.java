
package io.quarkus.container.image.docker.deployment;

import java.util.List;
import java.util.Optional;

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
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
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

public class DockerProcessor extends CommonProcessor<DockerConfig> {
    private static final Logger LOG = Logger.getLogger(DockerProcessor.class);
    private static final String DOCKER = "docker";
    static final String DOCKER_CONTAINER_IMAGE_NAME = "docker";

    @Override
    protected String getProcessorImplementation() {
        return DOCKER;
    }

    @BuildStep
    public AvailableContainerImageExtensionBuildItem availability() {
        return new AvailableContainerImageExtensionBuildItem(DOCKER);
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, DockerBuild.class }, onlyIfNot = NativeBuild.class)
    public void dockerBuildFromJar(DockerConfig dockerConfig,
            DockerStatusBuildItem dockerStatusBuildItem,
            ContainerImageConfig containerImageConfig,
            OutputTargetBuildItem out,
            ContainerImageInfoBuildItem containerImageInfo,
            @SuppressWarnings("unused") CompiledJavaVersionBuildItem compiledJavaVersion,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            @SuppressWarnings("unused") Optional<AppCDSResultBuildItem> appCDSResult, // ensure docker build will be performed after AppCDS creation
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder,
            PackageConfig packageConfig,
            @SuppressWarnings("unused") JarBuildItem jar // used to ensure that the jar has been built
    ) {

        buildFromJar(dockerConfig, dockerStatusBuildItem, containerImageConfig, out, containerImageInfo,
                buildRequest, pushRequest, artifactResultProducer, containerImageBuilder, packageConfig,
                ContainerRuntime.DOCKER, ContainerRuntime.PODMAN);
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, NativeBuild.class, DockerBuild.class })
    public void dockerBuildFromNativeImage(DockerConfig dockerConfig,
            DockerStatusBuildItem dockerStatusBuildItem,
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

        buildFromNativeImage(dockerConfig, dockerStatusBuildItem, containerImageConfig, containerImage,
                buildRequest, pushRequest, out, artifactResultProducer, containerImageBuilder, packageConfig, nativeImage,
                ContainerRuntime.DOCKER, ContainerRuntime.PODMAN);
    }

    @Override
    protected String createContainerImage(ContainerImageConfig containerImageConfig,
            DockerConfig dockerConfig,
            ContainerImageInfoBuildItem containerImageInfo,
            OutputTargetBuildItem out,
            DockerfilePaths dockerfilePaths,
            boolean buildContainerImage,
            boolean pushContainerImage,
            PackageConfig packageConfig,
            String executableName) {

        boolean useBuildx = dockerConfig.buildx().useBuildx();

        // useBuildx: Whether any of the buildx parameters are set
        //
        // pushImages: Whether the user requested the built images to be pushed to a registry
        // Pushing images is different based on if you're using buildx or not.
        // If not using any of the buildx params (useBuildx == false), then the flow is as it was before:
        //
        // 1) Build the image (docker build)
        // 2) Apply any tags (docker tag)
        // 3) Push the image and all tags (docker push)
        //
        // If using any of the buildx options (useBuildx == true), the tagging & pushing happens
        // as part of the 'docker buildx build' command via the added -t and --push params (see the getDockerArgs method).
        //
        // This is because when using buildx with more than one platform, the resulting images are not loaded into 'docker images'.
        // Therefore, a docker tag or docker push will not work after a docker build.

        if (useBuildx && pushContainerImage) {
            // Needed because buildx will push all the images in a single step
            loginToRegistryIfNeeded(containerImageConfig, containerImageInfo, executableName);
        }

        if (buildContainerImage) {
            var dockerBuildArgs = getDockerBuildArgs(containerImageInfo.getImage(), dockerfilePaths, containerImageConfig,
                    dockerConfig, containerImageInfo, pushContainerImage, executableName);

            buildImage(containerImageInfo, out, executableName, dockerBuildArgs, false);

            dockerConfig.buildx().platform()
                    .filter(platform -> !platform.isEmpty())
                    .ifPresentOrElse(
                            platform -> LOG.infof("Built container image %s (%s platform(s))\n", containerImageInfo.getImage(),
                                    String.join(",", platform)),
                            () -> LOG.infof("Built container image %s\n", containerImageInfo.getImage()));

            // If we didn't use buildx, now we need to process any tags
            if (!useBuildx && !containerImageInfo.getAdditionalImageTags().isEmpty()) {
                createAdditionalTags(containerImageInfo.getImage(), containerImageInfo.getAdditionalImageTags(),
                        executableName);
            }
        }

        if (!useBuildx && pushContainerImage) {
            // If not using buildx, push the images
            loginToRegistryIfNeeded(containerImageConfig, containerImageInfo, executableName);
            pushImages(containerImageInfo, executableName, dockerConfig);
        }

        return containerImageInfo.getImage();
    }

    @Override
    protected String getExecutableName(DockerConfig config, ContainerRuntime... containerRuntimes) {
        var executableName = super.getExecutableName(config, containerRuntimes);

        if (!DOCKER.equals(executableName)) {
            LOG.warnf(
                    "Using executable %s within the quarkus-container-image-%s extension. Maybe you should use the quarkus-container-image-%s extension instead?",
                    executableName, DOCKER, executableName);
        }

        return executableName;
    }

    private String[] getDockerBuildArgs(String image,
            DockerfilePaths dockerfilePaths,
            ContainerImageConfig containerImageConfig,
            DockerConfig dockerConfig,
            ContainerImageInfoBuildItem containerImageInfo,
            boolean pushImages,
            String executableName) {

        var dockerBuildArgs = getContainerCommonBuildArgs(image, dockerfilePaths, containerImageConfig, dockerConfig, true);
        var buildx = dockerConfig.buildx();
        var useBuildx = buildx.useBuildx();

        if (useBuildx) {
            // Check the executable. If not 'docker', then fail the build
            if (!DOCKER.equals(executableName)) {
                throw new IllegalArgumentException(
                        "The 'buildx' properties are specific to 'executable-name=docker' and can not be used with the '%s' executable name. Either remove the `buildx` properties or the `executable-name` property."
                                .formatted(executableName));
            }

            dockerBuildArgs.add(0, "buildx");
        }

        buildx.platform()
                .filter(platform -> !platform.isEmpty())
                .ifPresent(platform -> {
                    dockerBuildArgs.addAll(List.of("--platform", String.join(",", platform)));

                    if (platform.size() == 1) {
                        // Buildx only supports loading the image to the docker system if there is only 1 image
                        dockerBuildArgs.add("--load");
                    }
                });

        buildx.progress().ifPresent(progress -> dockerBuildArgs.addAll(List.of("--progress", progress)));
        buildx.output().ifPresent(output -> dockerBuildArgs.addAll(List.of("--output", output)));

        if (useBuildx) {
            // When using buildx for multi-arch images, it wants to push in a single step
            // 1) Create all the additional tags
            containerImageInfo.getAdditionalImageTags()
                    .forEach(additionalImageTag -> dockerBuildArgs.addAll(List.of("-t", additionalImageTag)));

            if (pushImages) {
                // 2) Enable the --push flag
                dockerBuildArgs.add("--push");
            }
        }

        dockerBuildArgs.add(dockerfilePaths.dockerExecutionPath().toAbsolutePath().toString());
        return dockerBuildArgs.toArray(String[]::new);
    }
}
