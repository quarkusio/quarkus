
package io.quarkus.container.image.docker.deployment;

import static io.quarkus.container.util.PathsUtil.findMainSourcesRoot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.util.NativeBinaryUtil;
import io.quarkus.container.spi.AvailableContainerImageExtensionBuildItem;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormalNotRemoteDev;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.AppCDSResultBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
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

    private final IsDockerWorking isDockerWorking = new IsDockerWorking();

    @BuildStep
    public AvailableContainerImageExtensionBuildItem availability() {
        return new AvailableContainerImageExtensionBuildItem(DOCKER);
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, DockerBuild.class }, onlyIfNot = NativeBuild.class)
    public void dockerBuildFromJar(DockerConfig dockerConfig,
            ContainerImageConfig containerImageConfig,
            OutputTargetBuildItem out,
            ContainerImageInfoBuildItem containerImageInfo,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            @SuppressWarnings("unused") Optional<AppCDSResultBuildItem> appCDSResult, // ensure docker build will be performed after AppCDS creation
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            PackageConfig packageConfig,
            @SuppressWarnings("unused") // used to ensure that the jar has been built
            JarBuildItem jar) {

        if (!containerImageConfig.build && !containerImageConfig.push && !buildRequest.isPresent()
                && !pushRequest.isPresent()) {
            return;
        }

        if (!isDockerWorking.getAsBoolean()) {
            throw new RuntimeException("Unable to build docker image. Please check your docker installation");
        }

        log.info("Building docker image for jar.");

        ImageIdReader reader = new ImageIdReader();
        String builtContainerImage = createContainerImage(containerImageConfig, dockerConfig, containerImageInfo, out, reader,
                false,
                pushRequest.isPresent(), packageConfig);

        // a pull is not required when using this image locally because the docker strategy always builds the container image
        // locally before pushing it to the registry
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container",
                Map.of("container-image", builtContainerImage, "pull-required", "false")));
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, NativeBuild.class, DockerBuild.class })
    public void dockerBuildFromNativeImage(DockerConfig dockerConfig,
            ContainerImageConfig containerImageConfig,
            ContainerImageInfoBuildItem containerImage,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            OutputTargetBuildItem out,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            PackageConfig packageConfig,
            // used to ensure that the native binary has been built
            NativeImageBuildItem nativeImage) {

        if (!containerImageConfig.build && !containerImageConfig.push && !buildRequest.isPresent()
                && !pushRequest.isPresent()) {
            return;
        }

        if (!isDockerWorking.getAsBoolean()) {
            throw new RuntimeException("Unable to build docker image. Please check your docker installation");
        }

        if (!NativeBinaryUtil.nativeIsLinuxBinary(nativeImage)) {
            throw new RuntimeException(
                    "The native binary produced by the build is not a Linux binary and therefore cannot be used in a Linux container image. Consider adding \"quarkus.native.container-build=true\" to your configuration");
        }

        log.info("Starting docker image build");

        ImageIdReader reader = new ImageIdReader();
        String builtContainerImage = createContainerImage(containerImageConfig, dockerConfig, containerImage, out, reader, true,
                pushRequest.isPresent(), packageConfig);

        // a pull is not required when using this image locally because the docker strategy always builds the container image
        // locally before pushing it to the registry
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container",
                Map.of("container-image", builtContainerImage, "pull-required", "false")));
    }

    private String createContainerImage(ContainerImageConfig containerImageConfig, DockerConfig dockerConfig,
            ContainerImageInfoBuildItem containerImageInfo,
            OutputTargetBuildItem out, ImageIdReader reader, boolean forNative, boolean pushRequested,
            PackageConfig packageConfig) {

        DockerfilePaths dockerfilePaths = getDockerfilePaths(dockerConfig, forNative, packageConfig, out);
        String[] dockerArgs = getDockerArgs(containerImageInfo.getImage(), dockerfilePaths, containerImageConfig, dockerConfig);
        log.infof("Executing the following command to build docker image: '%s %s'", dockerConfig.executableName,
                String.join(" ", dockerArgs));
        boolean buildSuccessful = ExecUtil.exec(out.getOutputDirectory().toFile(), reader, dockerConfig.executableName,
                dockerArgs);
        if (!buildSuccessful) {
            throw dockerException(dockerArgs);
        }

        log.infof("Built container image %s (%s)\n", containerImageInfo.getImage(), reader.getImageId());

        if (!containerImageInfo.getAdditionalImageTags().isEmpty()) {
            createAdditionalTags(containerImageInfo.getImage(), containerImageInfo.getAdditionalImageTags(), dockerConfig);
        }

        if (pushRequested || containerImageConfig.push) {
            String registry = "docker.io";
            if (!containerImageInfo.getRegistry().isPresent()) {
                log.info("No container image registry was set, so 'docker.io' will be used");
            } else {
                registry = containerImageInfo.getRegistry().get();
            }
            // Check if we need to login first
            if (containerImageConfig.username.isPresent() && containerImageConfig.password.isPresent()) {
                boolean loginSuccessful = ExecUtil.exec(dockerConfig.executableName, "login", registry, "-u",
                        containerImageConfig.username.get(),
                        "-p" + containerImageConfig.password.get());
                if (!loginSuccessful) {
                    throw dockerException(new String[] { "-u", containerImageConfig.username.get(), "-p", "********" });
                }
            }

            List<String> imagesToPush = new ArrayList<>(containerImageInfo.getAdditionalImageTags());
            imagesToPush.add(containerImageInfo.getImage());
            for (String imageToPush : imagesToPush) {
                pushImage(imageToPush, dockerConfig);
            }
        }

        return containerImageInfo.getImage();
    }

    private String[] getDockerArgs(String image, DockerfilePaths dockerfilePaths, ContainerImageConfig containerImageConfig,
            DockerConfig dockerConfig) {
        List<String> dockerArgs = new ArrayList<>(6 + dockerConfig.buildArgs.size());
        dockerArgs.addAll(Arrays.asList("build", "-f", dockerfilePaths.getDockerfilePath().toAbsolutePath().toString()));
        for (Map.Entry<String, String> entry : dockerConfig.buildArgs.entrySet()) {
            dockerArgs.addAll(Arrays.asList("--build-arg", entry.getKey() + "=" + entry.getValue()));
        }
        for (Map.Entry<String, String> entry : containerImageConfig.labels.entrySet()) {
            dockerArgs.addAll(Arrays.asList("--label", String.format("%s=%s", entry.getKey(), entry.getValue())));
        }
        if (dockerConfig.cacheFrom.isPresent()) {
            List<String> cacheFrom = dockerConfig.cacheFrom.get();
            if (!cacheFrom.isEmpty()) {
                dockerArgs.add("--cache-from");
                dockerArgs.add(String.join(",", cacheFrom));
            }
        }
        dockerArgs.addAll(Arrays.asList("-t", image));
        dockerArgs.add(dockerfilePaths.getDockerExecutionPath().toAbsolutePath().toString());
        return dockerArgs.toArray(new String[0]);
    }

    private void createAdditionalTags(String image, List<String> additionalImageTags, DockerConfig dockerConfig) {
        for (String additionalTag : additionalImageTags) {
            String[] tagArgs = { "tag", image, additionalTag };
            boolean tagSuccessful = ExecUtil.exec(dockerConfig.executableName, tagArgs);
            if (!tagSuccessful) {
                throw dockerException(tagArgs);
            }
        }
    }

    private void pushImage(String image, DockerConfig dockerConfig) {
        String[] pushArgs = { "push", image };
        boolean pushSuccessful = ExecUtil.exec(dockerConfig.executableName, pushArgs);
        if (!pushSuccessful) {
            throw dockerException(pushArgs);
        }
        log.info("Successfully pushed docker image " + image);
    }

    private RuntimeException dockerException(String[] dockerArgs) {
        return new RuntimeException(
                "Execution of 'docker " + String.join(" ", dockerArgs) + "' failed. See docker output for more details");
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
            } else if (packageConfig.type.equals(PackageConfig.LEGACY_JAR)) {
                return DockerfileDetectionResult.detect(DOCKERFILE_LEGACY_JAR, outputDirectory);
            } else {
                return DockerfileDetectionResult.detect(DOCKERFILE_JVM, outputDirectory);
            }
        }
    }

    /**
     * A function that creates a command output reader, that reads and holds the image id from the docker build output.
     */
    private static class ImageIdReader implements Function<InputStream, Runnable> {

        private final AtomicReference<String> id = new AtomicReference<>();

        public String getImageId() {
            return id.get();
        }

        @Override
        public Runnable apply(InputStream t) {
            return new Runnable() {
                @Override
                public void run() {
                    try (InputStreamReader isr = new InputStreamReader(t);
                            BufferedReader reader = new BufferedReader(isr)) {

                        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                            if (line.startsWith("Successfully built")) {
                                String[] parts = line.split(" ");
                                if (parts.length == 3)
                                    id.set(parts[2]);
                            }
                            log.info(line);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
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
                        "Specified Dockerfile path " + effectiveDockerfilePath.toAbsolutePath().toString() + " does not exist");
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
