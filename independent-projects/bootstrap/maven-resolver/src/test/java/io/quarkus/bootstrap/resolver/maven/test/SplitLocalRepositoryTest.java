package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;

public class SplitLocalRepositoryTest extends BootstrapMavenContextTestBase {
    @Test
    public void test() throws Exception {
        BootstrapMavenContext mvn = bootstrapMavenContextForProject("custom-settings/split-local-repository");
        LocalRepositoryManager lrm = mvn.getRepositorySystemSession().getLocalRepositoryManager();

        assertEquals("installed/releases/foo/bar/1.0/bar-1.0.jar",
                lrm.getPathForLocalArtifact(new DefaultArtifact("foo:bar:1.0")));
        assertEquals("installed/snapshots/foo/bar/1.0-SNAPSHOT/bar-1.0-SNAPSHOT.jar",
                lrm.getPathForLocalArtifact(new DefaultArtifact("foo:bar:1.0-SNAPSHOT")));

        RemoteRepository remoteRepo = new RemoteRepository.Builder("remote-repo", "default", "https://example.com/repo/")
                .build();

        assertEquals("cached/releases/foo/bar/1.0/bar-1.0.jar",
                lrm.getPathForRemoteArtifact(new DefaultArtifact("foo:bar:1.0"), remoteRepo, null));
        assertEquals("cached/snapshots/foo/bar/1.0-SNAPSHOT/bar-1.0-SNAPSHOT.jar",
                lrm.getPathForRemoteArtifact(new DefaultArtifact("foo:bar:1.0-SNAPSHOT"), remoteRepo, null));
    }
}
