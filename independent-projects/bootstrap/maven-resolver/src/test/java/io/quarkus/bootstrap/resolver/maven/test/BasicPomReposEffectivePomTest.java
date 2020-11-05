package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class BasicPomReposEffectivePomTest extends BootstrapMavenContextTestBase {

    @Test
    public void basicPomRepos() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("effective-pom/basic-pom-repos");
        Files.createDirectories(mvn.getCurrentProjectBaseDir().resolve("target"));
        assertEquals(
                Arrays.asList(
                        newRepo("central", "https://pom.central"),
                        newRepo("other-pom-repo", "https://pom.other")),
                mvn.getRemoteRepositories());
    }
}
