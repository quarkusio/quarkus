package io.quarkus.gcp.functions.http.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;

public class CloudFunctionsDeploymentBuildStep {
    /**
     * Creates a target/deployment dir and copy the uber jar in it.
     * This facilitates the usage of the 'gcloud' command.
     */
    @BuildStep(onlyIf = IsNormal.class, onlyIfNot = NativeBuild.class)
    public ArtifactResultBuildItem functionDeployment(OutputTargetBuildItem target, JarBuildItem jar)
            throws BuildException, IOException {
        if (!jar.isUberJar()) {
            throw new BuildException("Google Cloud Function deployment need to use a uberjar, " +
                    "please set 'quarkus.package.jar.type=uber-jar' inside your application.properties",
                    List.of());
        }

        Path deployment = target.getOutputDirectory().resolve("deployment");
        if (Files.notExists(deployment)) {
            Files.createDirectory(deployment);
        }

        Path jarPath = jar.getPath();
        Path targetJarPath = deployment.resolve(jarPath.getFileName());
        Files.deleteIfExists(targetJarPath);
        Files.copy(jarPath, targetJarPath);

        return new ArtifactResultBuildItem(targetJarPath, "function", Map.of());
    }
}
