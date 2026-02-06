package io.quarkus.test.junit.launcher;

import java.nio.file.Path;
import java.util.Properties;

import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.junit.QuarkusTestProfile;

public interface ArtifactLauncherProvider {

    /**
     * Determines whether this provider support the artifact type
     *
     */
    boolean supportsArtifactType(String type, String testProfile);

    /**
     * Returns an instance of {@link ArtifactLauncher} on which the {@code init} method has been called
     */
    ArtifactLauncher<? extends ArtifactLauncher.InitContext> create(CreateContext context);

    interface CreateContext {

        Properties quarkusArtifactProperties();

        Path buildOutputDirectory();

        Class<?> testClass();

        Class<? extends QuarkusTestProfile> profile();

        ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult();
    }

}
