package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import java.util.Arrays;
import java.util.List;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

public class ProxyAndMirrorSettingsReposTest extends BootstrapMavenContextTestBase {

    @Test
    public void basicPomRepos() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextWithSettings("custom-settings/proxy-and-mirror");

        final List<RemoteRepository> repos = mvn.getRemoteRepositories();
        assertEquals(2, repos.size());

        assertEquals("custom-repo", repos.get(0).getId());
        assertNotNull(repos.get(0).getProxy());
        assertNotNull(repos.get(0).getMirroredRepositories());

        final RemoteRepository centralRepo = repos.get(repos.size() - 1);
        assertEquals("mirror-A", centralRepo.getId(), "Central repo must be substitute by mirror");
        assertNotNull(centralRepo.getProxy());
        assertEquals(2, centralRepo.getMirroredRepositories().size());
        final List<String> mirrored = Arrays.asList("central", "jboss-public-repository");
        for (RemoteRepository repo : centralRepo.getMirroredRepositories()) {
            assertTrue(mirrored.contains(repo.getId()));
        }
    }
}
