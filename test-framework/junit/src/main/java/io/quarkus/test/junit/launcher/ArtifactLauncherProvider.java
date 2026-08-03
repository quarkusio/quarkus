package io.quarkus.test.junit.launcher;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

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

    /**
     * Providers are consulted in descending priority order, and the first one whose
     * {@link #supportsArtifactType(String, String)} returns {@code true} is used. Providers with the same priority keep
     * their {@link ServiceLoader} order. Opt-in providers that must take precedence over the built-in ones regardless
     * of where they appear on the classpath (e.g. one shipped in a separate artifact) should return a higher value.
     */
    default int priority() {
        return 0;
    }

    /**
     * Loads all the available providers, ordered as described in {@link #priority()}.
     */
    static List<ArtifactLauncherProvider> loadAll() {
        List<ArtifactLauncherProvider> providers = new ArrayList<>();
        ServiceLoader.load(ArtifactLauncherProvider.class).forEach(providers::add);
        // List.sort is stable, so providers with the same priority keep their ServiceLoader order
        providers.sort(Comparator.comparingInt(ArtifactLauncherProvider::priority).reversed());
        return providers;
    }

    interface CreateContext {

        Properties quarkusArtifactProperties();

        Path buildOutputDirectory();

        Class<?> testClass();

        Class<? extends QuarkusTestProfile> profile();

        ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult();
    }

}
