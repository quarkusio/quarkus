package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContextConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

public class PomReposMirroredTest extends BootstrapMavenContextTestBase {

    private static final String projectPath = "custom-settings/pom-repos-mirrored";

    private static final Map<String, String> MIRRORED;
    static {
        final Map<String, String> tmp = new HashMap<>();
        tmp.put("jboss-public-repository", "https://repository.jboss.org/nexus/content/repositories/releases/");
        tmp.put("custom", "https://custom-repo.org/repo");
        tmp.put("central", "https://repo.maven.apache.org/maven2");
        MIRRORED = tmp;
    }

    protected BootstrapMavenContextConfig<?> initBootstrapMavenContextConfig() throws Exception {
        final Path projectLocation = getProjectLocation(projectPath);
        return BootstrapMavenContext.config()
                .setWorkspaceDiscovery(true)
                .setCurrentProject(projectLocation.toString());
    }

    @Test
    public void basicPomRepos() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextWithSettings("custom-settings/pom-repos-mirrored");
        Files.createDirectories(mvn.getCurrentProjectBaseDir().resolve("target"));

        final List<RemoteRepository> repos = mvn.getRemoteRepositories();
        assertEquals(1, repos.size());

        RemoteRepository repo = repos.get(0);
        assertEquals("private-repo", repo.getId());
        assertNull(repo.getProxy());
        assertNotNull(repo.getAuthentication());

        List<RemoteRepository> mirroredRepos = repo.getMirroredRepositories();
        assertNotNull(mirroredRepos);
        assertEquals(MIRRORED.size(), mirroredRepos.size());
        for (RemoteRepository r : mirroredRepos) {
            assertTrue(MIRRORED.containsKey(r.getId()));
            assertEquals(MIRRORED.get(r.getId()), r.getUrl());
        }
    }
}
