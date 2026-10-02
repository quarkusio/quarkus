package io.quarkus.test.junit.launcher;

import java.nio.file.Path;
import java.util.Properties;

import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Service-provider contract for creating an integration-test launcher for a packaged artifact type.
 */
public interface ArtifactLauncherProvider {

    /**
     * Determines whether this provider supports an artifact type and test profile.
     *
     * @param type the artifact type recorded in the Quarkus artifact metadata
     * @param testProfile the integration-test configuration profile name
     * @return {@code true} when this provider can create a launcher for the artifact
     */
    boolean supportsArtifactType(String type, String testProfile);

    /**
     * Creates and initializes an artifact launcher.
     *
     * @param context artifact metadata and integration-test launch context
     * @return an initialized launcher
     */
    ArtifactLauncher<? extends ArtifactLauncher.InitContext> create(CreateContext context);

    /**
     * Values available while an artifact launcher is selected and created.
     */
    interface CreateContext {

        /**
         * @return metadata describing the packaged Quarkus artifact
         */
        Properties quarkusArtifactProperties();

        /**
         * @return the build output directory against which relative artifact metadata paths are resolved
         */
        Path buildOutputDirectory();

        /**
         * @return the integration-test class requesting the launcher
         */
        Class<?> testClass();

        /**
         * @return the selected test-profile class, or {@code null} when none is selected
         */
        Class<? extends QuarkusTestProfile> profile();

        /**
         * @return the Dev Services values and resources associated with this launch
         */
        ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult();
    }

}
