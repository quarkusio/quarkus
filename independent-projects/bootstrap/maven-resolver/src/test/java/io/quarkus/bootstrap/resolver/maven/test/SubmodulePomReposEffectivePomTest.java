package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class SubmodulePomReposEffectivePomTest extends BootstrapMavenContextTestBase {

    @Test
    public void basicPomRepos() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("effective-pom/submodule-repos/module");
        Files.createDirectories(mvn.getCurrentProjectBaseDir().resolve("target"));
        assertEquals(
                Arrays.asList(
                        newRepo("module-pom-repo", "https://module-pom.repo"),
                        newRepo("parent-pom-repo", "https://parent-pom.repo"),
                        BootstrapMavenContext.newDefaultRepository()),
                mvn.getRemoteRepositories());
    }
}
