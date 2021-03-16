package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class PomProfileReposEffectivePomTest extends BootstrapMavenContextTestBase {

    @Test
    public void basicPomRepos() throws Exception {
        setSystemProp("another-profile", "yes");
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("effective-pom/pom-profile-repos");
        Files.createDirectories(mvn.getCurrentProjectBaseDir().resolve("target"));
        assertEquals(
                Arrays.asList(
                        newRepo("another-profile-repo", "https://another-profile.repo"),
                        newRepo("central", "https://pom.central"),
                        newRepo("other-pom-repo", "https://pom.other")),
                mvn.getRemoteRepositories());
    }
}
