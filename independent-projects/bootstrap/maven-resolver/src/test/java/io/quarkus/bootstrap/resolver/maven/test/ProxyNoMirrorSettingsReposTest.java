package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import java.util.List;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

public class ProxyNoMirrorSettingsReposTest extends BootstrapMavenContextTestBase {

    @Test
    public void basicPomRepos() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextWithSettings("custom-settings/proxy-no-mirror");

        final List<RemoteRepository> repos = mvn.getRemoteRepositories();
        assertEquals(3, repos.size());

        assertEquals("custom-repo", repos.get(0).getId());
        assertNotNull(repos.get(0).getProxy());
        assertNotNull(repos.get(0).getMirroredRepositories());

        final RemoteRepository centralRepo = repos.get(repos.size() - 1);
        assertEquals("central", centralRepo.getId(), "central repo must be added as default repository");
        assertNotNull(centralRepo.getProxy());
        assertTrue(centralRepo.getMirroredRepositories().isEmpty());
    }
}
