
package io.quarkus.container.image.docker.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.ContainerImageConfig.Execution;
import io.quarkus.container.image.deployment.util.ImageUtil;
import io.quarkus.container.image.deployment.util.NativeBinaryUtil;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.container.spi.ContainerImageResultBuildItem;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.util.ExecUtil;

public class DockerProcessor {

    private static final Logger log = Logger.getLogger(DockerProcessor.class);
    private static final String DOCKERFILE_JVM = "Dockerfile.jvm";
    private static final String DOCKERFILE_NATIVE = "Dockerfile.native";

    @BuildStep(onlyIf = { IsNormal.class, DockerBuild.class }, onlyIfNot = NativeBuild.class)
    public void dockerBuildFromJar(DockerConfig dockerConfig,
            ContainerImageConfig containerImageConfig, // TODO: use to check whether we need to also push to registry
            OutputTargetBuildItem out,
            ContainerImageInfoBuildItem containerImage,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageResultBuildItem> containerImageResultProducer,
            // used to ensure that the jar has been built
            JarBuildItem jar) {

        if (containerImageConfig.execution == Execution.NONE && !buildRequest.isPresent() && !pushRequest.isPresent()) {
            return;
        }

        log.info("Building docker image for jar.");

        String image = containerImage.getImage();

        ImageIdReader reader = new ImageIdReader();
        createContainerImage(containerImageConfig, dockerConfig, image, out, reader, false, pushRequest.isPresent());

        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container", Collections.emptyMap()));
        containerImageResultProducer.produce(new ContainerImageResultBuildItem(reader.getImageId(),
                ImageUtil.getRepository(image), ImageUtil.getTag(image)));
    }

    @BuildStep(onlyIf = { IsNormal.class, DockerBuild.class, NativeBuild.class })
    public void dockerBuildFromNativeImage(DockerConfig dockerConfig,
            ContainerImageConfig containerImageConfig,
            ContainerImageInfoBuildItem containerImage,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            OutputTargetBuildItem out,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageResultBuildItem> containerImageResultProducer,
            // used to ensure that the native binary has been built
            NativeImageBuildItem nativeImage) {

        if (containerImageConfig.execution == Execution.NONE && !buildRequest.isPresent() && !pushRequest.isPresent()) {
            return;
        }

        if (!NativeBinaryUtil.nativeIsLinuxBinary(nativeImage)) {
            throw new RuntimeException(
                    "The native binary produced by the build is not a Linux binary and therefore cannot be used in a Linux container image. Consider adding \"quarkus.native.container-build=true\" to your configuration");
        }

        log.info("Building docker image for native image.");

        String image = containerImage.getImage();

        ImageIdReader reader = new ImageIdReader();
        createContainerImage(containerImageConfig, dockerConfig, image, out, reader, true, pushRequest.isPresent());
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container", Collections.emptyMap()));
        containerImageResultProducer
                .produce(new ContainerImageResultBuildItem(reader.getImageId(), ImageUtil.getRepository(image),
                        ImageUtil.getTag(image)));
    }

    private void createContainerImage(ContainerImageConfig containerImageConfig, DockerConfig dockerConfig, String image,
            OutputTargetBuildItem out, ImageIdReader reader, boolean forNative, boolean pushRequested) {

        DockerfilePaths dockerfilePaths = getDockerfilePaths(dockerConfig, forNative, out);
        String[] buildArgs = { "build", "-f", dockerfilePaths.getDockerfilePath().toAbsolutePath().toString(), "-t", image,
                dockerfilePaths.getDockerExecutionPath().toAbsolutePath().toString() };
        boolean buildSuccessful = ExecUtil.exec(out.getOutputDirectory().toFile(), reader, "docker", buildArgs);
        if (!buildSuccessful) {
            throw dockerException(buildArgs);
        }

        if (pushRequested || containerImageConfig.execution == ContainerImageConfig.Execution.PUSH) {
            String[] pushArgs = { "push", image };
            boolean pushSuccessful = ExecUtil.exec("docker", pushArgs);
            if (!pushSuccessful) {
                throw dockerException(pushArgs);
            }
        }
    }

    private RuntimeException dockerException(String[] dockerArgs) {
        return new RuntimeException(
                "Execution of 'docker " + String.join(" ", dockerArgs) + "' failed. See docker output for more details");
    }

    private DockerfilePaths getDockerfilePaths(DockerConfig dockerConfig, boolean forNative,
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
                            if (line.startsWith("Succesfully built")) {
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
            Path dockerfilesRoot = mainSourcesRoot.getKey().resolve("docker");
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
            if (!dockerfilePath.toFile().exists()) {
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

    /**
     * Return a Map.Entry (which is used as a Tuple) containing the main sources root as the key
     * and the project root as the valyue
     */
    private static AbstractMap.SimpleEntry<Path, Path> findMainSourcesRoot(Path outputDirectory) {
        Path currentPath = outputDirectory;
        do {
            Path toCheck = currentPath.resolve(Paths.get("src", "main"));
            if (toCheck.toFile().exists()) {
                return new AbstractMap.SimpleEntry<>(toCheck, currentPath);
            }
            if (Files.exists(currentPath.getParent())) {
                currentPath = currentPath.getParent();
            } else {
                return null;
            }
        } while (true);
    }

}
