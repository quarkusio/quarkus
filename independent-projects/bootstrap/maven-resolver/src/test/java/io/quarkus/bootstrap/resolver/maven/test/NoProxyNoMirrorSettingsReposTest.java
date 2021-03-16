package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import java.util.List;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

public class NoProxyNoMirrorSettingsReposTest extends BootstrapMavenContextTestBase {

    @Test
    public void basicPomRepos() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextWithSettings("custom-settings/no-proxy-no-mirror");

        final List<RemoteRepository> repos = mvn.getRemoteRepositories();
        assertEquals(3, repos.size());

        assertEquals("custom-repo", repos.get(0).getId());
        assertNull(repos.get(0).getProxy());
        assertTrue(repos.get(0).getMirroredRepositories().isEmpty());

        final RemoteRepository centralRepo = repos.get(repos.size() - 1);
        assertEquals("central", centralRepo.getId());
        assertNull(centralRepo.getProxy());
        assertTrue(centralRepo.getMirroredRepositories().isEmpty());
    }
}
