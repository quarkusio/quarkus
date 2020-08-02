package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContextConfig;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

/**
 * This test makes sure that projects with packaging 'jar' that don't include any content
 * (i.e. neither classes nor resources) are still resolvable locally.
 * In this case, our workspace resolver will create an empty classes directory to represent
 * the resolved artifact.
 */
public class ResolveEmptyJarProjectArtifactTest extends BootstrapMavenContextTestBase {

    protected BootstrapMavenContextConfig<?> initBootstrapMavenContextConfig() throws Exception {
        return BootstrapMavenContext.config();
    }

    @Test
    public void test() throws Exception {
        final BootstrapMavenContext ctx = bootstrapMavenContextForProject("empty-jar/root");
        final MavenArtifactResolver resolver = new MavenArtifactResolver(ctx);
        final Artifact emptyJar = resolver.resolve(new DefaultArtifact("org.acme", "root", "", "jar", "1.0-SNAPSHOT"))
                .getArtifact();
        assertEquals(ctx.getCurrentProjectBaseDir().resolve("target").resolve("classes"), emptyJar.getFile().toPath());
    }
}
