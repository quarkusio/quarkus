package io.quarkus.gradle.application.internal.execution.worker;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.gradle.api.GradleException;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolution;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolutionCodec;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolutionResultHandler;

public abstract class ResolveImageReferencesWorker extends QuarkusWorker<ResolveImageReferencesWorkerParams> {

    private static final String CONTAINER_IMAGE_INFO = "io.quarkus.container.spi.ContainerImageInfoBuildItem";

    @Override
    public void execute() {
        try (CuratedApplication application = createAppCreationContext()) {
            AtomicReference<ImageReferenceResolution> result = new AtomicReference<>();
            AugmentAction action = application.createAugmentor();
            action.performCustomBuild(
                    ImageReferenceResolutionResultHandler.class.getName(),
                    new Consumer<ImageReferenceResolution>() {
                        @Override
                        public void accept(ImageReferenceResolution resolution) {
                            result.set(resolution);
                        }
                    },
                    CONTAINER_IMAGE_INFO);
            ImageReferenceResolution resolution = result.get();
            if (resolution == null) {
                throw new GradleException("Quarkus image-reference preflight did not produce a result");
            }
            new ImageReferenceResolutionCodec().write(
                    getParameters().getResolutionReceiptFile().get().getAsFile().toPath(), resolution);
        } catch (BootstrapException e) {
            throw new GradleException("Failed to resolve effective Quarkus container image references due to " + e, e);
        }
    }
}
