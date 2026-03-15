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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.json.JsonArray;
import io.quarkus.bootstrap.json.JsonObject;
import io.quarkus.bootstrap.json.JsonReader;
import io.quarkus.bootstrap.json.JsonString;
import io.quarkus.bootstrap.json.JsonValue;
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
import io.smallrye.common.process.ProcessBuilder;
import io.smallrye.common.process.ProcessExecutionException;

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

            Optional<BuiltContainerInfo> maybeBuiltContainerInfo = determineBuiltContainerInfo(executableName,
                    builtContainerImage);
            String workingDirectory = null;
            if (maybeBuiltContainerInfo.isPresent()) {
                workingDirectory = maybeBuiltContainerInfo.get().effectiveWorkingDirectory();
            }

            // a pull is not required when using this image locally because the strategy always builds the container image
            // locally before pushing it to the registry
            Map<String, String> metadata = new HashMap<>();
            metadata.put("container-image", builtContainerImage);
            metadata.put("pull-required", "false");
            if (workingDirectory != null) {
                metadata.put("working-directory", workingDirectory);
            }
            metadata.put("output-directory", out.getOutputDirectory().toAbsolutePath().toString());
            artifactResultProducer.produce(
                    new ArtifactResultBuildItem(
                            null,
                            "jar-container",
                            Collections.unmodifiableMap(metadata)));

            containerImageBuilder.produce(new ContainerImageBuilderBuildItem(getProcessorImplementation()));
        }
    }

    private Optional<BuiltContainerInfo> determineBuiltContainerInfo(String executableName, String builtContainerImage) {
        try {
            StringBuffer sb = new StringBuffer();
            ProcessBuilder.newBuilder(executableName)
                    .arguments(List.of("inspect", builtContainerImage))
                    .error().logOnSuccess(false).inherited()
                    .output().consumeLinesWith(8092, sb::append)
                    .run();

            JsonArray results = JsonReader.of(sb.toString()).read();
            JsonObject imageData = (JsonObject) results.value().get(0);

            JsonObject config = imageData.get("Config");

            List<String> entrypoints = new ArrayList<>();
            JsonValue entrypointValue = config.get("Entrypoint");
            if (entrypointValue instanceof JsonArray entrypointArray) {
                entrypointArray.value().forEach(entrypoint -> {
                    if (entrypoint instanceof JsonString s) {
                        entrypoints.add(s.value());
                    }
                });
            }

            String workingDir = null;
            JsonValue workingDirVal = config.get("WorkingDir");
            if (workingDirVal instanceof JsonString js) {
                workingDir = js.value();
            }

            Optional<String> baseImage = Optional.empty();

            JsonValue labelsVal = config.get("Labels");
            if (labelsVal instanceof JsonObject labels) {
                JsonValue baseNameLabelObj = labels.get("org.opencontainers.image.base.name");
                if (baseNameLabelObj instanceof JsonString s) {
                    baseImage = Optional.of(s.value());
                } else {
                    JsonValue nameObj = labels.get("name");
                    JsonValue versionObj = labels.get("version");
                    if ((nameObj instanceof JsonString n) && (versionObj instanceof JsonString v)) {
                        baseImage = Optional.of(n.value() + ":" + v.value());
                    }
                }
            }

            return Optional.of(new BuiltContainerInfo(baseImage, entrypoints, workingDir));
        } catch (ProcessExecutionException e) {
            LOGGER.warnf("Error while inspecting built container image %s", executableName);
            return Optional.empty();
        }
    }

    private record BuiltContainerInfo(Optional<String> baseImage, List<String> entrypoint, String workingDirectory) {

        private boolean isUbiOpenJdkImage() {
            if (baseImage.isPresent()) {
                String baseImageVal = baseImage().get();
                if (baseImageVal.contains("ubi") && baseImageVal.contains("openjdk")) {
                    if (!entrypoint.isEmpty()) {
                        return entrypoint.get(0).endsWith("run-java.sh");
                    }
                }
            }
            return false;
        }

        private String effectiveWorkingDirectory() {
            if (isUbiOpenJdkImage()) {
                return "/deployments";
            }
            return workingDirectory;
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
        if (containerImageConfig.username().isPresent() && containerImageConfig.password().isPresent()) {
            ProcessBuilder.newBuilder(executableName)
                    .arguments("login", registry, "-u", containerImageConfig.username().get(), "-p",
                            containerImageConfig.password().get())
                    .error().logOnSuccess(false).inherited()
                    .run();
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
        containerImageConfig.labels().forEach((k, v) -> args.addAll(List.of("--label", "%s=%s".formatted(k, v))));
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
                    ProcessBuilder.newBuilder(executableName).arguments(tagArgs)
                            .error().logOnSuccess(false).inherited()
                            .run();
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
        ProcessBuilder.newBuilder(executableName).arguments(createPushArgs(image, config))
                .error().logOnSuccess(false).inherited()
                .run();
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
        ProcessBuilder.newBuilder(executableName)
                .directory(out.getOutputDirectory())
                .arguments(args)
                .error().logOnSuccess(false).inherited()
                .run();

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
