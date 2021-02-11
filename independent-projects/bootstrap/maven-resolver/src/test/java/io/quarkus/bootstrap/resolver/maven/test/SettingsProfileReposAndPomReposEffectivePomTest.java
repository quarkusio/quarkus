package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class SettingsProfileReposAndPomReposEffectivePomTest extends BootstrapMavenContextTestBase {

    @Test
    public void settingsProfileReposAndPomRepos() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("effective-pom/settings-profile-and-pom-repos");
        Files.createDirectories(mvn.getCurrentProjectBaseDir().resolve("target"));
        assertEquals(
                Arrays.asList(
                        newRepo("settings-central", "https://settings.central"),
                        newRepo("central", "https://pom.central"),
                        newRepo("other-pom-repo", "https://pom.other")),
                mvn.getRemoteRepositories());
    }
}
