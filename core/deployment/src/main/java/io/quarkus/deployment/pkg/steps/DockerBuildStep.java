
package io.quarkus.deployment.pkg.steps;

import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.pkg.builditem.DockerBuild;
import io.quarkus.deployment.pkg.builditem.DockerImageBuildItem;
import io.quarkus.deployment.pkg.builditem.DockerImageResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.ModuleDirBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.util.ExecUtil;

public class DockerBuildStep {

    private static final Logger log = Logger.getLogger(DockerBuildStep.class);

    @BuildStep(onlyIf = DockerBuild.class, onlyIfNot = NativeBuild.class)
    public DockerImageResultBuildItem dockerBuildFromJar(ApplicationInfoBuildItem app, ModuleDirBuildItem moduledir,
            Optional<DockerImageBuildItem> dockerImage,
            JarBuildItem artifact) {
        log.info("Building docker image for jar.");
        ExecUtil.exec(moduledir.getPath().toFile(),
                "docker", "build",
                "-f",
                moduledir.getPath().resolve("src").resolve("main").resolve("docker").resolve("Dockerfile.jvm").toAbsolutePath()
                        .toString(),
                "-t", dockerImage.map(d -> d.getImage()).orElse(app.getName() + ":" + app.getVersion()),
                moduledir.getPath().toAbsolutePath().toString());

        return new DockerImageResultBuildItem(null, null, null);
    }

    @BuildStep(onlyIf = { DockerBuild.class, NativeBuild.class })
    public DockerImageResultBuildItem dockerBuildFromNaticeImage(ApplicationInfoBuildItem app, ModuleDirBuildItem moduledir,
            Optional<DockerImageBuildItem> dockerImage,
            NativeImageBuildItem nativeImage) {
        log.info("Building docker image for native image.");
        ExecUtil.exec(moduledir.getPath().toFile(),
                "docker", "build",
                "-f",
                moduledir.getPath().resolve("src").resolve("main").resolve("docker").resolve("Dockerfile.native")
                        .toAbsolutePath()
                        .toString(),
                "-t", dockerImage.map(d -> d.getImage()).orElse(app.getName() + ":" + app.getVersion() + "-native"),
                moduledir.getPath().toAbsolutePath().toString());
        return new DockerImageResultBuildItem(null, null, null);
    }
}
