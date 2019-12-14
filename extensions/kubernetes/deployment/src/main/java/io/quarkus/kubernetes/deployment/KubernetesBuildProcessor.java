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

    @BuildStep(onlyIf = IsNormal.class)
    public KubernetesImageBuildItem build(
            KubernetesConfig kubernetesConfig,
            OutputTargetBuildItem outputTargetBuildItem,
            Optional<KubernetesProjectBuildItem> kubernetesProjectBI,
            Optional<JarBuildItem> jarBuildItem) {

        if (!jarBuildItem.isPresent() || !kubernetesProjectBI.isPresent() || kubernetesConfig.skipBuild) {
            log.info("Kubernetes build phase is skipped");
            return null;
        }
        final JKubeLogger jKubeLogger = new JKubeLogger(log);
        final Path projectDirectory = outputTargetBuildItem.getOutputDirectory().getParent();
        final Path dockerFile = projectDirectory.resolve("src/main/docker/Dockerfile.jvm");
        final String imageName = String.format("%s/%s:%s", kubernetesProjectBI.get().getGroup(),
                kubernetesProjectBI.get().getName(), kubernetesProjectBI.get().getVersion());
        copyFiles(projectDirectory, dockerFile, imageName);
        final BuildConfiguration buildConfig = new BuildConfiguration.Builder()
                .dockerFile(dockerFile.toFile().getAbsolutePath())
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

    private static void copyFiles(Path projectDir, Path dockerFile, String imageName) {
        final String targetDirName = "target";
        final Path buildDir = projectDir.resolve(targetDirName).resolve(imageName.replace(':', '/'))
                .resolve("build");
        final File buildTargetDir = buildDir.resolve(targetDirName).toFile();
        if (buildTargetDir.mkdirs()) {
            final FileFilter jarFilter = ff -> ff.getName().endsWith(".jar");
            Stream.of(Objects.requireNonNull(projectDir.resolve(targetDirName).toFile().listFiles(jarFilter)))
                    .forEach(copyFileRelative(buildTargetDir.toPath()));
            Stream.of(Objects.requireNonNull(projectDir.resolve(targetDirName).resolve("lib").toFile().listFiles(jarFilter)))
                    .forEach(copyFileRelative(buildTargetDir.toPath().resolve("lib")));
            try {
                Files.copy(dockerFile, buildDir.resolve("Dockerfile"));
            } catch (IOException e) {
                log.error("Error copying source files", e);
            }
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
