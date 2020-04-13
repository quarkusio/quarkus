package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class CentralRepoOverridesEffectivePomTest extends BootstrapMavenContextTestBase {

    @Test
    public void basicPomRepos() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("effective-pom/central-repo-overrides");
        assertEquals(
                Arrays.asList(
                        newRepo("central", "https://settings.central"),
                        newRepo("other-pom-repo", "https://pom.other")),
                mvn.getRemoteRepositories());
    }
}
