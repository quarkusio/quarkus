package io.quarkus.gradle.application.internal.image;

import java.util.Optional;

import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;

public final class StartupOptimizedContainerImageResultFactory {

    public static final String RESULT_TYPE = "startup-optimized-container-image";

    public BuiltContainerImage image(BuiltContainerImage baseImage, Optional<QuarkusApplicationImageBuilder> builder,
            boolean pushed, String optimizedImageReference) {
        return new BuiltContainerImage(
                RESULT_TYPE,
                builder,
                pushed,
                Optional.of(optimizedImageReference),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                baseImage.workingDirectory(),
                baseImage.outputDirectory());
    }
}
