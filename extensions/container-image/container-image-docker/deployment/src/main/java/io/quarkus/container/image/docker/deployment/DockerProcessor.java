
package io.quarkus.container.image.docker.deployment;

import static io.quarkus.container.image.deployment.util.EnablementUtil.buildContainerImageNeeded;
import static io.quarkus.container.image.deployment.util.EnablementUtil.pushContainerImageNeeded;
import static io.quarkus.container.util.PathsUtil.findMainSourcesRoot;
import static io.quarkus.deployment.util.ContainerRuntimeUtil.detectContainerRuntime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.util.NativeBinaryUtil;
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
import io.quarkus.deployment.util.ExecUtil;

public class DockerProcessor {

    private static final Logger log = Logger.getLogger(DockerProcessor.class);
    private static final String DOCKER = "docker";
    private static final String DOCKERFILE_JVM = "Dockerfile.jvm";
    private static final String DOCKERFILE_LEGACY_JAR = "Dockerfile.legacy-jar";
    private static final String DOCKERFILE_NATIVE = "Dockerfile.native";
    private static final String DOCKER_DIRECTORY_NAME = "docker";
    static final String DOCKER_CONTAINER_IMAGE_NAME = "docker";

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
            CompiledJavaVersionBuildItem compiledJavaVersion,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            @SuppressWarnings("unused") Optional<AppCDSResultBuildItem> appCDSResult, // ensure docker build will be performed after AppCDS creation
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder,
            PackageConfig packageConfig,
            @SuppressWarnings("unused") // used to ensure that the jar has been built
            JarBuildItem jar) {

        boolean buildContainerImage = buildContainerImageNeeded(containerImageConfig, buildRequest);
        boolean pushContainerImage = pushContainerImageNeeded(containerImageConfig, pushRequest);
        if (!buildContainerImage && !pushContainerImage) {
            return;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            throw new RuntimeException("Unable to build docker image. Please check your docker installation");
        }

        DockerfilePaths dockerfilePaths = getDockerfilePaths(dockerConfig, false, packageConfig, out);
        DockerFileBaseInformationProvider dockerFileBaseInformationProvider = DockerFileBaseInformationProvider.impl();
        Optional<DockerFileBaseInformationProvider.DockerFileBaseInformation> dockerFileBaseInformation = dockerFileBaseInformationProvider
                .determine(dockerfilePaths.getDockerfilePath());

        if (dockerFileBaseInformation.isPresent() && (dockerFileBaseInformation.get().getJavaVersion() < 17)) {
            throw new IllegalStateException(
                    String.format(
                            "The project is built with Java 17 or higher, but the selected Dockerfile (%s) is using a lower Java version in the base image (%s). Please ensure you are using the proper base image in the Dockerfile.",
                            dockerfilePaths.getDockerfilePath().toAbsolutePath(),
                            dockerFileBaseInformation.get().getBaseImage()));
        }

        if (buildContainerImage) {
            log.info("Starting (local) container image build for jar using docker.");
        }

        String builtContainerImage = createContainerImage(containerImageConfig, dockerConfig, containerImageInfo, out,
                false,
                buildContainerImage,
                pushContainerImage, packageConfig);

        // a pull is not required when using this image locally because the docker strategy always builds the container image
        // locally before pushing it to the registry
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container",
                Map.of("container-image", builtContainerImage, "pull-required", "false")));
        containerImageBuilder.produce(new ContainerImageBuilderBuildItem(DOCKER));
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, NativeBuild.class, DockerBuild.class })
    public void dockerBuildFromNativeImage(DockerConfig dockerConfig,
            DockerStatusBuildItem dockerStatusBuildItem,
            ContainerImageConfig containerImageConfig,
            ContainerImageInfoBuildItem containerImage,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            OutputTargetBuildItem out,
            Optional<UpxCompressedBuildItem> upxCompressed, // used to ensure that we work with the compressed native binary if compression was enabled
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder,
            PackageConfig packageConfig,
            // used to ensure that the native binary has been built
            NativeImageBuildItem nativeImage) {

        boolean buildContainerImage = buildContainerImageNeeded(containerImageConfig, buildRequest);
        boolean pushContainerImage = pushContainerImageNeeded(containerImageConfig, pushRequest);
        if (!buildContainerImage && !pushContainerImage) {
            return;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            throw new RuntimeException("Unable to build docker image. Please check your docker installation");
        }

        if (!NativeBinaryUtil.nativeIsLinuxBinary(nativeImage)) {
            throw new RuntimeException(
                    "The native binary produced by the build is not a Linux binary and therefore cannot be used in a Linux container image. Consider adding \"quarkus.native.container-build=true\" to your configuration");
        }

        log.info("Starting (local) container image build for native binary using docker.");

        String builtContainerImage = createContainerImage(containerImageConfig, dockerConfig, containerImage, out, true,
                buildContainerImage,
                pushContainerImage, packageConfig);

        // a pull is not required when using this image locally because the docker strategy always builds the container image
        // locally before pushing it to the registry
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container",
                Map.of("container-image", builtContainerImage, "pull-required", "false")));

        containerImageBuilder.produce(new ContainerImageBuilderBuildItem(DOCKER));
    }

    private String createContainerImage(ContainerImageConfig containerImageConfig, DockerConfig dockerConfig,
            ContainerImageInfoBuildItem containerImageInfo,
            OutputTargetBuildItem out, boolean forNative, boolean buildContainerImage,
            boolean pushContainerImage,
            PackageConfig packageConfig) {

        boolean useBuildx = dockerConfig.buildx.useBuildx();

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

        DockerfilePaths dockerfilePaths = getDockerfilePaths(dockerConfig, forNative, packageConfig, out);
        String[] dockerArgs = getDockerArgs(containerImageInfo.getImage(), dockerfilePaths, containerImageConfig, dockerConfig,
                containerImageInfo, pushContainerImage);

        if (useBuildx && pushContainerImage) {
            // Needed because buildx will push all the images in a single step
            loginToRegistryIfNeeded(containerImageConfig, containerImageInfo, dockerConfig);
        }

        if (buildContainerImage) {
            final String executableName = dockerConfig.executableName.orElse(detectContainerRuntime(true).getExecutableName());
            log.infof("Executing the following command to build docker image: '%s %s'", executableName,
                    String.join(" ", dockerArgs));
            boolean buildSuccessful = ExecUtil.exec(out.getOutputDirectory().toFile(), executableName, dockerArgs);
            if (!buildSuccessful) {
                throw dockerException(executableName, dockerArgs);
            }

            dockerConfig.buildx.platform
                    .filter(platform -> platform.size() > 1)
                    .ifPresentOrElse(
                            platform -> log.infof("Built container image %s (%s platform(s))\n", containerImageInfo.getImage(),
                                    String.join(",", platform)),
                            () -> log.infof("Built container image %s\n", containerImageInfo.getImage()));

        }

        if (!useBuildx && buildContainerImage) {
            // If we didn't use buildx, now we need to process any tags
            if (!containerImageInfo.getAdditionalImageTags().isEmpty()) {
                createAdditionalTags(containerImageInfo.getImage(), containerImageInfo.getAdditionalImageTags(), dockerConfig);
            }
        }

        if (!useBuildx && pushContainerImage) {
            // If not using buildx, push the images
            loginToRegistryIfNeeded(containerImageConfig, containerImageInfo, dockerConfig);

            Stream.concat(containerImageInfo.getAdditionalImageTags().stream(), Stream.of(containerImageInfo.getImage()))
                    .forEach(imageToPush -> pushImage(imageToPush, dockerConfig));
        }

        return containerImageInfo.getImage();
    }

    private void loginToRegistryIfNeeded(ContainerImageConfig containerImageConfig,
            ContainerImageInfoBuildItem containerImageInfo, DockerConfig dockerConfig) {
        String registry = containerImageInfo.getRegistry()
                .orElseGet(() -> {
                    log.info("No container image registry was set, so 'docker.io' will be used");
                    return "docker.io";
                });

        // Check if we need to login first
        if (containerImageConfig.username.isPresent() && containerImageConfig.password.isPresent()) {
            final String executableName = dockerConfig.executableName.orElse(detectContainerRuntime(true).getExecutableName());
            boolean loginSuccessful = ExecUtil.exec(executableName, "login", registry, "-u",
                    containerImageConfig.username.get(),
                    "-p" + containerImageConfig.password.get());
            if (!loginSuccessful) {
                throw dockerException(executableName,
                        new String[] { "-u", containerImageConfig.username.get(), "-p", "********" });
            }
        }
    }

    private String[] getDockerArgs(String image, DockerfilePaths dockerfilePaths, ContainerImageConfig containerImageConfig,
            DockerConfig dockerConfig, ContainerImageInfoBuildItem containerImageInfo, boolean pushImages) {
        List<String> dockerArgs = new ArrayList<>(6 + dockerConfig.buildArgs.size() + dockerConfig.additionalArgs.map(
                List::size).orElse(0));
        boolean useBuildx = dockerConfig.buildx.useBuildx();

        if (useBuildx) {
            final String executableName = dockerConfig.executableName.orElse(detectContainerRuntime(true).getExecutableName());
            // Check the executable. If not 'docker', then fail the build
            if (!DOCKER.equals(executableName)) {
                throw new IllegalArgumentException(
                        String.format(
                                "The 'buildx' properties are specific to 'executable-name=docker' and can not be used with " +
                                        "the '%s' executable name. Either remove the `buildx` properties or the `executable-name` property.",
                                executableName));
            }

            dockerArgs.add("buildx");
        }

        dockerArgs.addAll(Arrays.asList("build", "-f", dockerfilePaths.getDockerfilePath().toAbsolutePath().toString()));
        dockerConfig.buildx.platform
                .filter(platform -> !platform.isEmpty())
                .ifPresent(platform -> {
                    dockerArgs.add("--platform");
                    dockerArgs.add(String.join(",", platform));

                    if (platform.size() == 1) {
                        // Buildx only supports loading the image to the docker system if there is only 1 image
                        dockerArgs.add("--load");
                    }
                });
        dockerConfig.buildx.progress.ifPresent(progress -> dockerArgs.addAll(List.of("--progress", progress)));
        dockerConfig.buildx.output.ifPresent(output -> dockerArgs.addAll(List.of("--output", output)));
        dockerConfig.buildArgs
                .forEach((key, value) -> dockerArgs.addAll(Arrays.asList("--build-arg", String.format("%s=%s", key, value))));
        containerImageConfig.labels
                .forEach((key, value) -> dockerArgs.addAll(Arrays.asList("--label", String.format("%s=%s", key, value))));
        dockerConfig.cacheFrom
                .filter(cacheFrom -> !cacheFrom.isEmpty())
                .ifPresent(cacheFrom -> {
                    dockerArgs.add("--cache-from");
                    dockerArgs.add(String.join(",", cacheFrom));
                });
        dockerConfig.network.ifPresent(network -> {
            dockerArgs.add("--network");
            dockerArgs.add(network);
        });
        dockerConfig.additionalArgs.ifPresent(dockerArgs::addAll);
        dockerArgs.addAll(Arrays.asList("-t", image));

        if (useBuildx) {
            // When using buildx for multi-arch images, it wants to push in a single step
            // 1) Create all the additional tags
            containerImageInfo.getAdditionalImageTags()
                    .forEach(additionalImageTag -> dockerArgs.addAll(List.of("-t", additionalImageTag)));

            if (pushImages) {
                // 2) Enable the --push flag
                dockerArgs.add("--push");
            }
        }

        dockerArgs.add(dockerfilePaths.getDockerExecutionPath().toAbsolutePath().toString());
        return dockerArgs.toArray(new String[0]);
    }

    private void createAdditionalTags(String image, List<String> additionalImageTags, DockerConfig dockerConfig) {
        final String executableName = dockerConfig.executableName.orElse(detectContainerRuntime(true).getExecutableName());
        for (String additionalTag : additionalImageTags) {
            String[] tagArgs = { "tag", image, additionalTag };
            boolean tagSuccessful = ExecUtil.exec(executableName, tagArgs);
            if (!tagSuccessful) {
                throw dockerException(executableName, tagArgs);
            }
        }
    }

    private void pushImage(String image, DockerConfig dockerConfig) {
        final String executableName = dockerConfig.executableName.orElse(detectContainerRuntime(true).getExecutableName());
        String[] pushArgs = { "push", image };
        boolean pushSuccessful = ExecUtil.exec(executableName, pushArgs);
        if (!pushSuccessful) {
            throw dockerException(executableName, pushArgs);
        }
        log.info("Successfully pushed docker image " + image);
    }

    private RuntimeException dockerException(String executableName, String[] dockerArgs) {
        return new RuntimeException(
                "Execution of '" + executableName + " " + String.join(" ", dockerArgs)
                        + "' failed. See docker output for more details");
    }

    private DockerfilePaths getDockerfilePaths(DockerConfig dockerConfig, boolean forNative,
            PackageConfig packageConfig,
            OutputTargetBuildItem outputTargetBuildItem) {
        Path outputDirectory = outputTargetBuildItem.getOutputDirectory();
        if (forNative) {
            if (dockerConfig.dockerfileNativePath.isPresent()) {
                return ProvidedDockerfile.get(Paths.get(dockerConfig.dockerfileNativePath.get()), outputDirectory);
            } else {
                return DockerfileDetectionResult.detect(DOCKERFILE_NATIVE, outputDirectory);
            }
        } else {
            if (dockerConfig.dockerfileJvmPath.isPresent()) {
                return ProvidedDockerfile.get(Paths.get(dockerConfig.dockerfileJvmPath.get()), outputDirectory);
            } else if (packageConfig.isLegacyJar()) {
                return DockerfileDetectionResult.detect(DOCKERFILE_LEGACY_JAR, outputDirectory);
            } else {
                return DockerfileDetectionResult.detect(DOCKERFILE_JVM, outputDirectory);
            }
        }
    }

    private interface DockerfilePaths {
        Path getDockerfilePath();

        Path getDockerExecutionPath();
    }

    private static class DockerfileDetectionResult implements DockerfilePaths {
        private final Path dockerfilePath;
        private final Path dockerExecutionPath;

        private DockerfileDetectionResult(Path dockerfilePath, Path dockerExecutionPath) {
            this.dockerfilePath = dockerfilePath;
            this.dockerExecutionPath = dockerExecutionPath;
        }

        public Path getDockerfilePath() {
            return dockerfilePath;
        }

        public Path getDockerExecutionPath() {
            return dockerExecutionPath;
        }

        static DockerfileDetectionResult detect(String resource, Path outputDirectory) {
            Map.Entry<Path, Path> dockerfileToExecutionRoot = findDockerfileRoot(outputDirectory);
            if (dockerfileToExecutionRoot == null) {
                throw new IllegalStateException(
                        "Unable to find root of Dockerfile files. Consider adding 'src/main/docker/' to your project root");
            }
            Path dockerFilePath = dockerfileToExecutionRoot.getKey().resolve(resource);
            if (!Files.exists(dockerFilePath)) {
                throw new IllegalStateException(
                        "Unable to find Dockerfile " + resource + " in "
                                + dockerfileToExecutionRoot.getKey().toAbsolutePath());
            }
            return new DockerfileDetectionResult(dockerFilePath, dockerfileToExecutionRoot.getValue());
        }

        private static Map.Entry<Path, Path> findDockerfileRoot(Path outputDirectory) {
            Map.Entry<Path, Path> mainSourcesRoot = findMainSourcesRoot(outputDirectory);
            if (mainSourcesRoot == null) {
                return null;
            }
            Path dockerfilesRoot = mainSourcesRoot.getKey().resolve(DOCKER_DIRECTORY_NAME);
            if (!dockerfilesRoot.toFile().exists()) {
                return null;
            }
            return new AbstractMap.SimpleEntry<>(dockerfilesRoot, mainSourcesRoot.getValue());
        }

    }

    private static class ProvidedDockerfile implements DockerfilePaths {
        private final Path dockerfilePath;
        private final Path dockerExecutionPath;

        private ProvidedDockerfile(Path dockerfilePath, Path dockerExecutionPath) {
            this.dockerfilePath = dockerfilePath;
            this.dockerExecutionPath = dockerExecutionPath;
        }

        public static ProvidedDockerfile get(Path dockerfilePath, Path outputDirectory) {
            AbstractMap.SimpleEntry<Path, Path> mainSourcesRoot = findMainSourcesRoot(outputDirectory);
            if (mainSourcesRoot == null) {
                throw new IllegalStateException("Unable to determine project root");
            }
            Path effectiveDockerfilePath = dockerfilePath.isAbsolute() ? dockerfilePath
                    : mainSourcesRoot.getValue().resolve(dockerfilePath);
            if (!effectiveDockerfilePath.toFile().exists()) {
                throw new IllegalArgumentException(
                        "Specified Dockerfile path " + effectiveDockerfilePath.toAbsolutePath() + " does not exist");
            }
            return new ProvidedDockerfile(
                    effectiveDockerfilePath,
                    mainSourcesRoot.getValue());
        }

        @Override
        public Path getDockerfilePath() {
            return dockerfilePath;
        }

        @Override
        public Path getDockerExecutionPath() {
            return dockerExecutionPath;
        }
    }

}
