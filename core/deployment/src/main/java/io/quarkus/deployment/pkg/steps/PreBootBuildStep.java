package io.quarkus.deployment.pkg.steps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.runner.QuarkusEntryPoint;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.utilities.JavaBinFinder;

public class PreBootBuildStep {

    private static final Logger log = Logger.getLogger(PreBootBuildStep.class);

    /**
     * This build step actually launches the fast jar but enables the guard that makes sure that only static-init
     * the {@link io.quarkus.runtime.StartupTask} operations are run.
     * The point of this is to capture the classpath resources that are loaded by the aforementioned operations and
     * thus build another index which can be used by the {@link io.quarkus.bootstrap.runner.RunnerClassLoader}
     * in order to allow it load the those resources at runtime from the exact jars instead of simply relying on
     * the index of the directory structure of those jars.
     * Building this additional index requires additional support by {@link io.quarkus.bootstrap.runner.SerializedApplication}
     * and {@link QuarkusEntryPoint}
     */
    @BuildStep
    void preBootFastJar(PackageConfig packageConfig, JarBuildItem jarResult,
            BuildProducer<ArtifactResultBuildItem> artifactResult) {
        if (!packageConfig.isFastJar() || !packageConfig.preBoot) {
            return;
        }

        List<String> command = new ArrayList<>(6);
        command.add(JavaBinFinder.findBin());
        command.add("-D" + QuarkusEntryPoint.PRE_BOOT_SYSTEM_PROPERTY + "=true");
        command.add("-jar");
        command.add(JarResultBuildStep.QUARKUS_RUN_JAR);

        Path workingDirectory = jarResult.getPath().getParent();

        if (log.isDebugEnabled()) {
            log.debugf("Launching command: '%s' to create additional RunnerClassLoader index.", String.join(" ", command));
        }

        int exitCode;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile());
            if (log.isDebugEnabled()) {
                processBuilder.inheritIO();
            } else {
                processBuilder.redirectError(StepUtil.NULL_FILE);
                processBuilder.redirectOutput(StepUtil.NULL_FILE);
            }
            exitCode = processBuilder.start().waitFor();
        } catch (Exception e) {
            log.warn("Failed to launch process used to create additional RunnerClassLoader index.", e);
            return;
        }
        if (exitCode != 0) {
            log.warnf("The process that was supposed to create additional RunnerClassLoader index exited with error code: %d.",
                    exitCode);
            return;
        } else {
            log.debug("Create of additional RunnerClassLoader index complete.");
        }

        artifactResult.produce(
                new ArtifactResultBuildItem(workingDirectory.resolve(QuarkusEntryPoint.QUARKUS_RUNNER_CL_ADDITIONAL_INDEX_DAT),
                        "additionalRunnerClassloaderIndex", Collections.emptyMap()));
    }
}
