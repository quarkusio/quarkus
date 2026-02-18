package io.quarkus.gradle.tasks.worker;

import java.nio.file.Path;
import java.util.Map;

import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.deployment.cmd.BuildAotEnhancedCustomizerProducer;
import io.quarkus.deployment.cmd.BuildEnhancedAotContainerImageCommandHandler;
import io.quarkus.deployment.pkg.builditem.BuildAotOptimizedContainerImageResultBuildItem;

public abstract class BuildAotEnhancedImageWorker extends QuarkusWorker<BuildAotEnhancedImageWorkerParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildAotEnhancedImageWorker.class);

    @Override
    public void execute() {
        BuildAotEnhancedImageWorkerParams params = getParameters();

        String originalContainerImage = params.getOriginalContainerImage().get();
        String containerWorkingDirectory = params.getContainerWorkingDirectory().get();
        Path aotFile = params.getAotFile().getAsFile().get().toPath();

        try (CuratedApplication curatedApplication = createAppCreationContext()) {
            Map<String, Object> context = Map.of(
                    "original-container-image", originalContainerImage,
                    "container-working-directory", containerWorkingDirectory,
                    "aot-file", aotFile);

            AugmentAction action = curatedApplication.createAugmentor(
                    BuildAotEnhancedCustomizerProducer.class.getName(),
                    context);

            action.performCustomBuild(
                    BuildEnhancedAotContainerImageCommandHandler.class.getName(),
                    null,
                    BuildAotOptimizedContainerImageResultBuildItem.class.getName());

            LOGGER.info("AOT enhanced container image built successfully");
        } catch (BootstrapException e) {
            throw new GradleException("Failed to build AOT enhanced container image: " + e.getMessage(), e);
        }
    }
}
