package io.quarkus.container.image.docker.common.deployment;

import static io.quarkus.container.image.deployment.util.EnablementUtil.buildContainerImageNeeded;
import static io.quarkus.container.image.deployment.util.EnablementUtil.pushContainerImageNeeded;
import static io.quarkus.container.util.PathsUtil.findMainSourcesRoot;
import static io.quarkus.deployment.util.ContainerRuntimeUtil.detectContainerRuntime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.util.NativeBinaryUtil;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageBuilderBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ContainerRuntimeStatusBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;
import io.quarkus.deployment.util.ExecUtil;

public abstract class CommonProcessor<C extends CommonConfig> {
    private static final Logger LOGGER = Logger.getLogger(CommonProcessor.class);
    protected static final String DOCKERFILE_JVM = "Dockerfile.jvm";
    protected static final String DOCKERFILE_LEGACY_JAR = "Dockerfile.legacy-jar";
    protected static final String DOCKERFILE_NATIVE = "Dockerfile.native";
    protected static final String DOCKER_DIRECTORY_NAME = "docker";

    protected abstract String getProcessorImplementation();

    protected abstract String createContainerImage(ContainerImageConfig containerImageConfig, C config,
            ContainerImageInfoBuildItem containerImageInfo, OutputTargetBuildItem out, DockerfilePaths dockerfilePaths,
            boolean buildContainerImage, boolean pushContainerImage, PackageConfig packageConfig, String executableName);

    protected void buildFromJar(C config,
            ContainerRuntimeStatusBuildItem containerRuntimeStatusBuildItem,
            ContainerImageConfig containerImageConfig,
            OutputTargetBuildItem out,
            ContainerImageInfoBuildItem containerImageInfo,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder,
            PackageConfig packageConfig,
            ContainerRuntime... containerRuntimes) {

        var buildContainerImage = buildContainerImageNeeded(containerImageConfig, buildRequest);
        var pushContainerImage = pushContainerImageNeeded(containerImageConfig, pushRequest);

        if (buildContainerImage || pushContainerImage) {
            if (!containerRuntimeStatusBuildItem.isContainerRuntimeAvailable()) {
                throw new RuntimeException(
                        "Unable to build container image. Please check your %s installation."
                                .formatted(getProcessorImplementation()));
            }

            var dockerfilePaths = getDockerfilePaths(config, false, packageConfig, out);
            var dockerfileBaseInformation = DockerFileBaseInformationProvider.impl()
                    .determine(dockerfilePaths.dockerfilePath());

            if (dockerfileBaseInformation.isPresent() && (dockerfileBaseInformation.get().javaVersion() < 17)) {
                throw new IllegalStateException(
                        "The project is built with Java 17 or higher, but the selected Dockerfile (%s) is using a lower Java version in the base image (%s). Please ensure you are using the proper base image in the Dockerfile."
                                .formatted(
                                        dockerfilePaths.dockerfilePath().toAbsolutePath(),
                                        dockerfileBaseInformation.get().baseImage()));
            }

            if (buildContainerImage) {
                LOGGER.infof("Starting (local) container image build for jar using %s", getProcessorImplementation());
            }

            var executableName = getExecutableName(config, containerRuntimes);
            var builtContainerImage = createContainerImage(containerImageConfig, config, containerImageInfo, out,
                    dockerfilePaths, buildContainerImage, pushContainerImage, packageConfig, executableName);

            // a pull is not required when using this image locally because the strategy always builds the container image
            // locally before pushing it to the registry
            artifactResultProducer.produce(
                    new ArtifactResultBuildItem(
                            null,
                            "jar-container",
                            Map.of(
                                    "container-image", builtContainerImage,
                                    "pull-required", "false")));

            containerImageBuilder.produce(new ContainerImageBuilderBuildItem(getProcessorImplementation()));
        }
    }

    protected void buildFromNativeImage(C config,
            ContainerRuntimeStatusBuildItem containerRuntimeStatusBuildItem,
            ContainerImageConfig containerImageConfig,
            ContainerImageInfoBuildItem containerImage,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            OutputTargetBuildItem out,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder,
            PackageConfig packageConfig,
            NativeImageBuildItem nativeImage,
            ContainerRuntime... containerRuntimes) {

        var buildContainerImage = buildContainerImageNeeded(containerImageConfig, buildRequest);
        var pushContainerImage = pushContainerImageNeeded(containerImageConfig, pushRequest);

        if (buildContainerImage || pushContainerImage) {
            if (!containerRuntimeStatusBuildItem.isContainerRuntimeAvailable()) {
                throw new RuntimeException(
                        "Unable to build container image. Please check your %s installation."
                                .formatted(getProcessorImplementation()));
            }

            if (!NativeBinaryUtil.nativeIsLinuxBinary(nativeImage)) {
                throw new RuntimeException(
                        "The native binary produced by the build is not a Linux binary and therefore cannot be used in a Linux container image. Consider adding \"quarkus.native.container-build=true\" to your configuration.");
            }

            if (buildContainerImage) {
                LOGGER.infof("Starting (local) container image build for jar using %s", getProcessorImplementation());
            }

            var executableName = getExecutableName(config, containerRuntimes);
            var dockerfilePaths = getDockerfilePaths(config, true, packageConfig, out);
            var builtContainerImage = createContainerImage(containerImageConfig, config, containerImage, out, dockerfilePaths,
                    buildContainerImage, pushContainerImage, packageConfig, executableName);

            // a pull is not required when using this image locally because the strategy always builds the container image
            // locally before pushing it to the registry
            artifactResultProducer.produce(
                    new ArtifactResultBuildItem(
                            null,
                            "native-container",
                            Map.of(
                                    "container-image", builtContainerImage,
                                    "pull-required", "false")));

            containerImageBuilder.produce(new ContainerImageBuilderBuildItem(getProcessorImplementation()));
        }
    }

    protected void loginToRegistryIfNeeded(ContainerImageConfig containerImageConfig,
            ContainerImageInfoBuildItem containerImageInfo,
            String executableName) {

        var registry = containerImageInfo.getRegistry()
                .orElseGet(() -> {
                    LOGGER.info("No container image registry was set, so 'docker.io' will be used");
                    return "docker.io";
                });

        // Check if we need to login first
        if (containerImageConfig.username.isPresent() && containerImageConfig.password.isPresent()) {
            var loginSuccessful = ExecUtil.exec(executableName, "login", registry, "-u", containerImageConfig.username.get(),
                    "-p", containerImageConfig.password.get());

            if (!loginSuccessful) {
                throw containerRuntimeException(executableName,
                        new String[] { "-u", containerImageConfig.username.get(), "-p", "********" });
            }
        }
    }

    protected List<String> getContainerCommonBuildArgs(String image,
            DockerfilePaths dockerfilePaths,
            ContainerImageConfig containerImageConfig,
            C config,
            boolean addImageAsTag) {

        var args = new ArrayList<String>(6 + config.buildArgs().size() + config.additionalArgs().map(List::size).orElse(0));
        args.addAll(List.of("build", "-f", dockerfilePaths.dockerfilePath().toAbsolutePath().toString()));

        config.buildArgs().forEach((k, v) -> args.addAll(List.of("--build-arg", "%s=%s".formatted(k, v))));
        containerImageConfig.labels.forEach((k, v) -> args.addAll(List.of("--label", "%s=%s".formatted(k, v))));
        config.cacheFrom()
                .filter(cacheFrom -> !cacheFrom.isEmpty())
                .ifPresent(cacheFrom -> args.addAll(List.of("--cache-from", String.join(",", cacheFrom))));
        config.network().ifPresent(network -> args.addAll(List.of("--network", network)));
        config.additionalArgs().ifPresent(args::addAll);

        if (addImageAsTag) {
            args.addAll(List.of("-t", image));
        }

        return args;
    }

    protected void createAdditionalTags(String image, List<String> additionalImageTags, String executableName) {
        additionalImageTags.stream()
                .map(additionalTag -> new String[] { "tag", image, additionalTag })
                .forEach(tagArgs -> {
                    LOGGER.infof("Running '%s %s'", executableName, String.join(" ", tagArgs));
                    var tagSuccessful = ExecUtil.exec(executableName, tagArgs);

                    if (!tagSuccessful) {
                        throw containerRuntimeException(executableName, tagArgs);
                    }
                });
    }

    protected void pushImages(ContainerImageInfoBuildItem containerImageInfo, String executableName, C config) {
        List<String> imagesToPush = new ArrayList<>(1 + containerImageInfo.getAdditionalImageTags().size());
        imagesToPush.add(containerImageInfo.getImage());
        imagesToPush.addAll(containerImageInfo.getAdditionalImageTags());
        for (String image : imagesToPush) {
            pushImage(image, executableName, config);
        }
    }

    protected void pushImage(String image, String executableName, C config) {
        String[] pushArgs = createPushArgs(image, config);
        var pushSuccessful = ExecUtil.exec(executableName, pushArgs);

        if (!pushSuccessful) {
            throw containerRuntimeException(executableName, pushArgs);
        }

        LOGGER.infof("Successfully pushed %s image %s", getProcessorImplementation(), image);
    }

    protected String[] createPushArgs(String image, C config) {
        return new String[] { "push", image };
    }

    protected void buildImage(ContainerImageInfoBuildItem containerImageInfo,
            OutputTargetBuildItem out,
            String executableName,
            String[] args,
            boolean createAdditionalTags) {

        LOGGER.infof("Executing the following command to build image: '%s %s'", executableName,
                String.join(" ", args));
        var buildSuccessful = ExecUtil.exec(out.getOutputDirectory().toFile(), executableName, args);

        if (!buildSuccessful) {
            throw containerRuntimeException(executableName, args);
        }

        if (createAdditionalTags && !containerImageInfo.getAdditionalImageTags().isEmpty()) {
            createAdditionalTags(containerImageInfo.getImage(), containerImageInfo.getAdditionalImageTags(),
                    executableName);
        }
    }

    protected RuntimeException containerRuntimeException(String executableName, String[] args) {
        return new RuntimeException(
                "Execution of '%s %s' failed. See %s output for more details"
                        .formatted(
                                executableName,
                                String.join(" ", args),
                                getProcessorImplementation()));
    }

    protected String getExecutableName(C config, ContainerRuntime... containerRuntimes) {
        return config.executableName()
                .orElseGet(() -> detectContainerRuntime(containerRuntimes).getExecutableName());
    }

    private DockerfilePaths getDockerfilePaths(C config,
            boolean forNative,
            PackageConfig packageConfig,
            OutputTargetBuildItem out) {

        var outputDirectory = out.getOutputDirectory();

        if (forNative) {
            return config.dockerfileNativePath()
                    .map(dockerfileNativePath -> ProvidedDockerfile.get(Paths.get(dockerfileNativePath), outputDirectory))
                    .orElseGet(() -> DockerfileDetectionResult.detect(DOCKERFILE_NATIVE, outputDirectory));
        } else {
            return config.dockerfileJvmPath()
                    .map(dockerfileJvmPath -> ProvidedDockerfile.get(Paths.get(dockerfileJvmPath), outputDirectory))
                    .orElseGet(() -> (packageConfig.jar().type() == JarType.LEGACY_JAR)
                            ? DockerfileDetectionResult.detect(DOCKERFILE_LEGACY_JAR, outputDirectory)
                            : DockerfileDetectionResult.detect(DOCKERFILE_JVM, outputDirectory));
        }
    }

    protected interface DockerfilePaths {
        Path dockerfilePath();

        Path dockerExecutionPath();
    }

    protected record DockerfileDetectionResult(Path dockerfilePath, Path dockerExecutionPath) implements DockerfilePaths {
        protected static DockerfilePaths detect(String resource, Path outputDirectory) {
            var dockerfileToExecutionRoot = findDockerfileRoot(outputDirectory);
            if (dockerfileToExecutionRoot == null) {
                throw new IllegalStateException(
                        "Unable to find root of Dockerfile files. Consider adding 'src/main/docker/' to your project root.");
            }

            var dockerFilePath = dockerfileToExecutionRoot.getKey().resolve(resource);
            if (!Files.exists(dockerFilePath)) {
                throw new IllegalStateException(
                        "Unable to find Dockerfile %s in %s"
                                .formatted(resource, dockerfileToExecutionRoot.getKey().toAbsolutePath()));
            }

            return new DockerfileDetectionResult(dockerFilePath, dockerfileToExecutionRoot.getValue());
        }

        private static Map.Entry<Path, Path> findDockerfileRoot(Path outputDirectory) {
            var mainSourcesRoot = findMainSourcesRoot(outputDirectory);
            if (mainSourcesRoot == null) {
                return null;
            }

            var dockerfilesRoot = mainSourcesRoot.getKey().resolve(DOCKER_DIRECTORY_NAME);
            if (!dockerfilesRoot.toFile().exists()) {
                return null;
            }

            return new AbstractMap.SimpleEntry<>(dockerfilesRoot, mainSourcesRoot.getValue());
        }
    }

    protected record ProvidedDockerfile(Path dockerfilePath, Path dockerExecutionPath) implements DockerfilePaths {
        protected static DockerfilePaths get(Path dockerfilePath, Path outputDirectory) {
            var mainSourcesRoot = findMainSourcesRoot(outputDirectory);

            if (mainSourcesRoot == null) {
                throw new IllegalStateException("Unable to determine project root");
            }

            var executionPath = mainSourcesRoot.getValue();
            var effectiveDockerfilePath = dockerfilePath.isAbsolute() ? dockerfilePath : executionPath.resolve(dockerfilePath);

            if (!effectiveDockerfilePath.toFile().exists()) {
                throw new IllegalArgumentException(
                        "Specified Dockerfile path %s does not exist".formatted(effectiveDockerfilePath.toAbsolutePath()));
            }

            return new ProvidedDockerfile(effectiveDockerfilePath, executionPath);
        }
    }
}
