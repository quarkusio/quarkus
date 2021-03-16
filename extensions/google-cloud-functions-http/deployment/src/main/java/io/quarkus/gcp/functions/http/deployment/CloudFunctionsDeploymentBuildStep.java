package io.quarkus.gcp.functions.http.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarRequiredBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;

public class CloudFunctionsDeploymentBuildStep {
    @BuildStep
    public UberJarRequiredBuildItem forceUberJar() {
        // Google Cloud Function needs a single JAR inside a dedicated directory
        return new UberJarRequiredBuildItem();
    }

    /**
     * Creates a target/deployment dir and copy the uber jar in it.
     * This facilitates the usage of the 'glcoud' command.
     */
    @BuildStep(onlyIf = IsNormal.class, onlyIfNot = NativeBuild.class)
    public ArtifactResultBuildItem functionDeployment(OutputTargetBuildItem target, JarBuildItem jar)
            throws BuildException, IOException {
        if (!jar.isUberJar()) {
            throw new BuildException("Google Cloud Function deployment need to use a uberjar, " +
                    "please set 'quarkus.package.type=uber-jar' inside your application.properties",
                    Collections.EMPTY_LIST);
        }

        Path deployment = target.getOutputDirectory().resolve("deployment");
        if (Files.notExists(deployment)) {
            Files.createDirectory(deployment);
        }

        Path jarPath = jar.getPath();
        Path targetJarPath = deployment.resolve(jarPath.getFileName());
        Files.deleteIfExists(targetJarPath);
        Files.copy(jarPath, targetJarPath);

        return new ArtifactResultBuildItem(targetJarPath, "function", Collections.EMPTY_MAP);
    }
}
