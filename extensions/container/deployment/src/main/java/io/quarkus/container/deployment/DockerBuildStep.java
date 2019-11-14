
package io.quarkus.container.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.pkg.builditem.ContainerImageBuildItem;
import io.quarkus.deployment.pkg.builditem.ContainerImageResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.util.ExecUtil;
import io.quarkus.deployment.util.ImageUtil;

public class DockerBuildStep {

    private static final Logger log = Logger.getLogger(DockerBuildStep.class);
    private static final String DOCKERFILE_JVM = "Dockerfile.jvm";
    private static final String DOCKERFILE_NATIVE = "Dockerfile.native";

    @BuildStep(onlyIf = DockerBuild.class, onlyIfNot = NativeBuild.class)
    public ContainerImageResultBuildItem dockerBuildFromJar(ApplicationInfoBuildItem app,
            OutputTargetBuildItem out,
            Optional<ContainerImageBuildItem> dockerImage,
            JarBuildItem artifact) {
        log.info("Building docker image for jar.");
        String image = dockerImage.map(d -> d.getImage()).orElse(app.getName() + ":" + app.getVersion());
        ImageIdReader reader = new ImageIdReader();
        Path dockerFile = extractDockerfile(DOCKERFILE_JVM);
        ExecUtil.exec(out.getOutputDirectory().toFile(),
                reader,
                "docker", "build",
                "-f",
                dockerFile.resolve(DOCKERFILE_JVM).toAbsolutePath().toString(),
                "-t", image,
                out.getOutputDirectory().toAbsolutePath().toString());

        return new ContainerImageResultBuildItem(reader.getImageId(), ImageUtil.getRepository(image), ImageUtil.getTag(image));
    }

    @BuildStep(onlyIf = { DockerBuild.class, NativeBuild.class })
    public ContainerImageResultBuildItem dockerBuildFromNativeImage(ApplicationInfoBuildItem app,
            OutputTargetBuildItem out,
            Optional<ContainerImageBuildItem> dockerImage,
            NativeImageBuildItem nativeImage) {
        log.info("Building docker image for native image.");
        String image = dockerImage.map(d -> d.getImage()).orElse(app.getName() + ":" + app.getVersion() + "-native");
        Path dockerFile = extractDockerfile(DOCKERFILE_NATIVE);
        ImageIdReader reader = new ImageIdReader();
        ExecUtil.exec(out.getOutputDirectory().toFile(),
                reader,
                "docker", "build",
                "-f",
                dockerFile.resolve(DOCKERFILE_NATIVE).toAbsolutePath().toString(),
                "-t", image,
                out.getOutputDirectory().toAbsolutePath().toString());
        return new ContainerImageResultBuildItem(reader.getImageId(), ImageUtil.getRepository(image), ImageUtil.getTag(image));
    }

    private Path extractDockerfile(String resource) {
        final Path path;
        try {
            path = Files.createTempDirectory("quarkus-docker");
        } catch (IOException e) {
            throw new RuntimeException("Unable to setup environment for generating docker resources", e);
        }

        try (InputStream jvm = getClass().getClassLoader().getResource(resource).openStream()) {
            Files.copy(jvm, path.resolve(resource), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to extract docker resource: " + resource, e);
        }
        return path;
    }

    /**
     * A function that creates a command output reader, that reads and holds the image id from the docker build output.
     */
    private class ImageIdReader implements Function<InputStream, Runnable> {

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
}
