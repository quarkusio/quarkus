package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;

/**
 * Verifies that a Maven 4-style settings.xml (which contains top-level
 * {@code <repositories>} and {@code <pluginRepositories>} elements unknown
 * to the Maven 3 settings parser) is parsed correctly and the top-level
 * repositories are included in the resolved remote repositories.
 *
 * <p>
 * Maven 4's SETTINGS/2.0.0 schema supports repositories outside of profiles.
 * The Maven 3 {@code DefaultSettingsBuilder} silently ignores these elements
 * during lenient parsing. Quarkus supplements this by parsing the raw XML
 * via StAX to extract and use the top-level repositories.
 */
public class Maven4SettingsTopLevelRepositoriesTest extends BootstrapMavenContextTestBase {

    @Test
    public void topLevelRepositoriesAreResolved() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextWithSettings(
                "custom-settings/maven4-top-level-repositories");

        // Settings parsed without exception
        assertNotNull(mvn.getEffectiveSettings());

        final List<RemoteRepository> repos = mvn.getRemoteRepositories();
        assertNotNull(repos);

        // Repositories from the profile should be present
        assertTrue(repos.stream().anyMatch(r -> "custom-repo".equals(r.getId())),
                "Expected 'custom-repo' from the settings profile, got: " + repos);

        // Top-level repository from Maven 4 settings should also be present
        assertTrue(repos.stream().anyMatch(r -> "maven4-top-level-repo".equals(r.getId())),
                "Expected 'maven4-top-level-repo' from top-level <repositories>, got: " + repos);
    }

    @Test
    public void topLevelRepositorySnapshotPolicyIsHonored() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextWithSettings(
                "custom-settings/maven4-top-level-repositories");

        final List<RemoteRepository> repos = mvn.getRemoteRepositories();

        final RemoteRepository topLevelRepo = repos.stream()
                .filter(r -> "maven4-top-level-repo".equals(r.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(topLevelRepo, "Expected 'maven4-top-level-repo' in resolved repositories");
        assertEquals("https://maven4.example.com/releases/", topLevelRepo.getUrl());
        assertNotNull(topLevelRepo.getPolicy(true), "Expected snapshot policy");
        assertEquals(false, topLevelRepo.getPolicy(true).isEnabled(),
                "Expected snapshots disabled for maven4-top-level-repo");
    }
}
