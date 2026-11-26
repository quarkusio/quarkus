package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;

public class SettingsProfileReposAndPomReposEffectivePomTest extends BootstrapMavenContextTestBase {

    @Test
    public void settingsProfileReposAndPomRepos() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("effective-pom/settings-profile-and-pom-repos");
        Files.createDirectories(mvn.getCurrentProjectBaseDir().resolve("target"));
        assertEquals(
                newResolutionRepos(mvn,
                        newRepo("common-repo", "https://common.settings"),
                        newRepo("settings-central", "https://settings.central"),
                        newRepo("central", "https://pom.central"),
                        newRepo("other-pom-repo", "https://pom.other")),
                mvn.getRemoteRepositories());
    }
}
