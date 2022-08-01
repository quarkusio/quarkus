package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContextConfig;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.junit.jupiter.api.Test;

public class PreferPomsFromWorkspaceTest extends BootstrapMavenContextTestBase {

    @Override
    protected void initBootstrapMavenContextConfig(BootstrapMavenContextConfig<?> config) {
        config.setPreferPomsFromWorkspace(true);
    }

    @Test
    public void preferPomsFromWorkspace() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-alternate-pom/root/module2");
        final Artifact artifact = new DefaultArtifact(mvn.getCurrentProject().getGroupId(),
                mvn.getCurrentProject().getArtifactId(), null, "pom", mvn.getCurrentProject().getVersion());
        final ArtifactDescriptorResult descriptor = mvn.getRepositorySystem().readArtifactDescriptor(
                mvn.getRepositorySystemSession(),
                new ArtifactDescriptorRequest().setArtifact(artifact).setRepositories(mvn.getRemoteRepositories()));
        final List<Dependency> managedDeps = descriptor.getManagedDependencies();
        assertEquals(1, managedDeps.size());
        assertDependency(managedDeps.get(0));
        assertEquals(1, descriptor.getDependencies().size());
        assertDependency(descriptor.getDependencies().get(0));
    }

    private void assertDependency(Dependency d) {
        assertEquals("acme-other", d.getArtifact().getArtifactId());
        assertEquals("1.0", d.getArtifact().getVersion());
    }
}
