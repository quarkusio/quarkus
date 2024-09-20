package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;

public class ChainedLocalRepositoryManagerTest extends BootstrapMavenContextTestBase {

    private static final String M2_LOCAL_1;
    private static final String M2_LOCAL_2;
    private static final String M2_FROM_REMOTE;

    static {
        final String projectLocation;
        try {
            projectLocation = getProjectLocation("workspace-with-local-repo-tail").toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        M2_LOCAL_1 = Paths.get(projectLocation, ".m2-local-1", "repository").toAbsolutePath().toString();
        M2_LOCAL_2 = Paths.get(projectLocation, ".m2-local-2", "repository").toAbsolutePath().toString();
        M2_FROM_REMOTE = Paths.get(projectLocation, ".m2-from-remote", "repository").toAbsolutePath().toString();
    }

    // Tail configuration tests

    @Test
    public void testNoTail() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail");
        assertThrowsExactly(BootstrapMavenException.class, () -> resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testTailConfiguredButEmptyString() throws Exception {
        setSystemProp("maven.repo.local.tail", "");
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail");
        assertThrowsExactly(BootstrapMavenException.class, () -> resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testTailConfiguredButBlank() throws Exception {
        setSystemProp("maven.repo.local.tail", "  ");
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail");
        assertThrowsExactly(BootstrapMavenException.class, () -> resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testTailConfiguredButNonExistent() throws Exception {
        setSystemProp("maven.repo.local.tail", "/tmp/this-dir-does-not-exist");
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail");
        assertThrowsExactly(BootstrapMavenException.class, () -> resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testValidTailViaSystemProp() throws Exception {
        setSystemProp("maven.repo.local.tail", M2_LOCAL_1);
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail");
        assertNotNull(resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testValidTailViaConfig() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail",
                BootstrapMavenContext.config()
                        .setLocalRepositoryTail(M2_LOCAL_1));

        assertNotNull(resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testValidTailResolutionOrder() throws Exception {
        final BootstrapMavenContext mvnLocal1first = bootstrapMavenContextForProject("workspace-with-local-repo-tail",
                BootstrapMavenContext.config()
                        .setLocalRepositoryTail(M2_LOCAL_1, M2_LOCAL_2));

        final BootstrapMavenContext mvnLocal2first = bootstrapMavenContextForProject("workspace-with-local-repo-tail",
                BootstrapMavenContext.config()
                        .setLocalRepositoryTail(M2_LOCAL_2, M2_LOCAL_1));

        assertEquals(resolveOrgAcmeFooJar001(mvnLocal1first).getFile().getAbsolutePath(),
                Paths.get(M2_LOCAL_1, "org", "acme", "foo", "0.0.1", "foo-0.0.1.jar").toAbsolutePath().toString());
        assertEquals(resolveOrgAcmeFooJar001(mvnLocal2first).getFile().getAbsolutePath(),
                Paths.get(M2_LOCAL_2, "org", "acme", "foo", "0.0.1", "foo-0.0.1.jar").toAbsolutePath().toString());
    }

    @Test
    public void testValidTailMultiplicity() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail",
                BootstrapMavenContext.config()
                        .setLocalRepositoryTail(M2_LOCAL_1, M2_LOCAL_2));

        final Artifact foo = resolveOrgAcmeFooJar001(mvn);
        assertNotNull(foo);
        assertEquals(foo.getFile().getAbsolutePath(),
                Paths.get(M2_LOCAL_1, "org", "acme", "foo", "0.0.1", "foo-0.0.1.jar").toAbsolutePath().toString());

        final Artifact bar = resolveOrgAcmeBarJar002(mvn);
        assertNotNull(bar);
        assertEquals(bar.getFile().getAbsolutePath(),
                Paths.get(M2_LOCAL_2, "org", "acme", "bar", "0.0.2", "bar-0.0.2.jar").toAbsolutePath().toString());
    }

    // ignoreAvailability tests

    @Test
    public void testValidTailLocalCheckingForAvailabilityViaConfig() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail",
                BootstrapMavenContext.config()
                        .setLocalRepositoryTailIgnoreAvailability(false)
                        .setLocalRepositoryTail(M2_LOCAL_1));

        assertNotNull(resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testValidTailFromRemoteCheckingForAvailabilityViaConfig() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail",
                BootstrapMavenContext.config()
                        .setLocalRepositoryTailIgnoreAvailability(false)
                        .setLocalRepositoryTail(M2_FROM_REMOTE));

        assertThrowsExactly(BootstrapMavenException.class, () -> resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testValidTailFromRemoteCheckingForAvailabilityViaSystemProp() throws Exception {
        setSystemProp("maven.repo.local.tail.ignoreAvailability", "false");
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail",
                BootstrapMavenContext.config()
                        .setLocalRepositoryTail(M2_FROM_REMOTE));

        assertThrowsExactly(BootstrapMavenException.class, () -> resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testValidTailFromRemoteIgnoringAvailabilityViaSystemPropEmpty() throws Exception {
        setSystemProp("maven.repo.local.tail.ignoreAvailability", ""); // will become `true`
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail",
                BootstrapMavenContext.config()
                        .setLocalRepositoryTail(M2_FROM_REMOTE));

        assertNotNull(resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testValidTailFromRemoteIgnoringAvailabilityViaSystemPropBlank() throws Exception {
        setSystemProp("maven.repo.local.tail.ignoreAvailability", " "); // will become `true`
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail",
                BootstrapMavenContext.config()
                        .setLocalRepositoryTail(M2_FROM_REMOTE));

        assertNotNull(resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testValidTailFromRemoteIgnoringAvailabilityViaSystemPropTruthy() throws Exception {
        setSystemProp("maven.repo.local.tail.ignoreAvailability", "fals"); // will become `true`
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail",
                BootstrapMavenContext.config()
                        .setLocalRepositoryTail(M2_FROM_REMOTE));

        assertNotNull(resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testValidTailLocalIgnoringAvailabilityViaConfig() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail",
                BootstrapMavenContext.config()
                        .setLocalRepositoryTailIgnoreAvailability(true)
                        .setLocalRepositoryTail(M2_LOCAL_1));

        assertNotNull(resolveOrgAcmeFooJar001(mvn));
    }

    @Test
    public void testValidTailFromRemoteIgnoringAvailabilityViaConfig() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextForProject("workspace-with-local-repo-tail",
                BootstrapMavenContext.config()
                        .setLocalRepositoryTailIgnoreAvailability(true)
                        .setLocalRepositoryTail(M2_FROM_REMOTE));

        assertNotNull(resolveOrgAcmeFooJar001(mvn));
    }

    private Artifact resolveOrgAcmeFooJar001(BootstrapMavenContext ctx) throws BootstrapMavenException {
        final MavenArtifactResolver resolver = new MavenArtifactResolver(ctx);
        return resolver.resolve(new DefaultArtifact("org.acme", "foo", "", "jar", "0.0.1")).getArtifact();
    }

    private Artifact resolveOrgAcmeBarJar002(BootstrapMavenContext ctx) throws BootstrapMavenException {
        final MavenArtifactResolver resolver = new MavenArtifactResolver(ctx);
        return resolver.resolve(new DefaultArtifact("org.acme", "bar", "", "jar", "0.0.2")).getArtifact();
    }
}
