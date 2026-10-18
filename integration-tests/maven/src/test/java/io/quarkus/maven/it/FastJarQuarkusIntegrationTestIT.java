package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

@DisableForNative
class FastJarQuarkusIntegrationTestIT extends QuarkusITBase {

    @Test
    void testFastJar() throws MavenInvocationException, IOException {
        doTest("qit-fast-jar", "fastjar");
    }

    @Test
    void testFastJarWithForceUseArtifactIdOnlyAsName() throws MavenInvocationException, IOException {
        File testDir = doTest("qit-fast-jar-forceUseArtifactId", "fastjar",
                "-Dquarkus.package.jar.force-use-artifact-id-only-as-name=io.quarkus:quarkus-reactive-routes");
        System.out.println(testDir);
        File forceNamedFile = new File(testDir, "target/quarkus-app/lib/main/quarkus-reactive-routes.jar");
        assertThat(forceNamedFile).exists();
    }
}
