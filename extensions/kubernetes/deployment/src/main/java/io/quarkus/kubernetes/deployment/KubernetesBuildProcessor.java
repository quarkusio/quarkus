package io.quarkus.kubernetes.deployment;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.JkubeServiceHub;
import org.jboss.logging.Logger;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesImageBuildItem;
import io.quarkus.kubernetes.spi.KubernetesProjectBuildItem;

public class KubernetesBuildProcessor extends AbstractJKubeProcessor {

    private static final Logger log = Logger.getLogger(KubernetesBuildProcessor.class);

    @Inject
    KubernetesConfig kubernetesConfig;

    @BuildStep(onlyIf = IsNormal.class)
    public KubernetesImageBuildItem build(
            OutputTargetBuildItem outputTargetBuildItem,
            Optional<KubernetesProjectBuildItem> kubernetesProjectBI,
            Optional<JarBuildItem> jarBuildItem) {

        if (!jarBuildItem.isPresent() || !kubernetesProjectBI.isPresent() || kubernetesConfig.skipBuild) {
            log.info("Kubernetes build phase is skipped");
            return null;
        }
        final JKubeLogger jKubeLogger = new JKubeLogger(log);
        final String imageName = String.format("%s/%s:%s", kubernetesProjectBI.get().getGroup(),
                kubernetesProjectBI.get().getName(), kubernetesProjectBI.get().getVersion());
        final Path projectDirectory = outputTargetBuildItem.getOutputDirectory().getParent();
        final Path buildDirectory = initBuildDir(projectDirectory, imageName);
        copyRequiredFiles(projectDirectory, buildDirectory);
        final BuildConfiguration buildConfig = new BuildConfiguration.Builder()
                .dockerFile(generateTargetDockerfile(projectDirectory, buildDirectory).toFile().getAbsolutePath())
                .workdir(projectDirectory.toString())
                .build();
        try {
            final JkubeServiceHub jkubeServiceHub = initJKubeServiceHub(jKubeLogger, projectDirectory);
            final ImageConfiguration imageConfiguration = new ImageConfiguration.Builder()
                    .name(imageName)
                    .buildConfig(buildConfig)
                    .build();
            imageConfiguration.initAndValidate(original -> imageName, jKubeLogger);
            jkubeServiceHub.getBuildService().build(imageConfiguration);
            return new KubernetesImageBuildItem(imageName);
        } catch (Exception e) {
            log.error("Error building Docker image", e);
        }
        return null;
    }

    private Path generateTargetDockerfile(Path projectDirectory, Path buildDir) {
        final Path sourceDockerFile = projectDirectory.resolve(kubernetesConfig.dockerFile);
        final Path targetDockerFile = buildDir.resolve("Dockerfile");
        try {
            Files.copy(sourceDockerFile, targetDockerFile);
        } catch (IOException e) {
            log.error("Error generating target Dockerfile", e);
        }
        return targetDockerFile;
    }

    private static Path initBuildDir(Path projectDir, String imageName) {
        final Path buildDir = projectDir.resolve(TARGET_DIR).resolve(imageName.replace(':', '/'))
                .resolve("build");
        buildDir.toFile().mkdirs();
        return buildDir;
    }

    private static void copyRequiredFiles(Path projectDir, Path buildDir) {
        final File buildTargetDir = buildDir.resolve(TARGET_DIR).toFile();
        if (buildTargetDir.mkdirs()) {
            final FileFilter jarFilter = ff -> ff.getName().endsWith(".jar");
            Stream.of(Objects.requireNonNull(projectDir.resolve(TARGET_DIR).toFile().listFiles(jarFilter)))
                    .forEach(copyFileRelative(buildTargetDir.toPath()));
            Stream.of(Objects.requireNonNull(projectDir.resolve(TARGET_DIR).resolve("lib").toFile().listFiles(jarFilter)))
                    .forEach(copyFileRelative(buildTargetDir.toPath().resolve("lib")));
        }
    }

    private static Consumer<? super File> copyFileRelative(Path targetDir) {
        targetDir.toFile().mkdirs();
        return srcFile -> {
            try {
                Files.copy(srcFile.toPath(),
                        targetDir.resolve(srcFile.toPath().getParent().relativize(srcFile.toPath())));
            } catch (IOException e) {
                log.errorf(e, "Error copying file %s", srcFile.getName());
            }
        };
    }

}
