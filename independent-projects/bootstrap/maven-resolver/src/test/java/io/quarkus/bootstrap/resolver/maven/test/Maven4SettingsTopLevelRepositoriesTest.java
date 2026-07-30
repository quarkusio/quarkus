package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;

/**
 * Verifies that a Maven 4-style settings.xml (which contains top-level
 * {@code <repositories>} and {@code <pluginRepositories>} elements unknown
 * to the Maven 3 settings parser) is parsed without throwing an exception.
 *
 * <p>
 * The Maven 3 {@code DefaultSettingsBuilder} handles this by falling back
 * from strict to lenient parsing, silently ignoring the unknown elements.
 * This test ensures that Quarkus does not escalate the resulting
 * {@code WARNING}-severity {@code SettingsProblem} into an error.
 */
public class Maven4SettingsTopLevelRepositoriesTest extends BootstrapMavenContextTestBase {

    @Test
    public void maven4SettingsWithTopLevelRepositoriesParsedSuccessfully() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextWithSettings(
                "custom-settings/maven4-top-level-repositories");

        // Settings parsed without exception
        assertNotNull(mvn.getEffectiveSettings());

        // Repositories from the profile are still resolved
        final List<RemoteRepository> repos = mvn.getRemoteRepositories();
        assertNotNull(repos);

        // Expect at least the custom-repo from the profile and central
        final boolean hasCustomRepo = repos.stream()
                .anyMatch(r -> "custom-repo".equals(r.getId()));
        assertEquals(true, hasCustomRepo,
                "Expected 'custom-repo' from the settings profile, got: " + repos);
    }
}
