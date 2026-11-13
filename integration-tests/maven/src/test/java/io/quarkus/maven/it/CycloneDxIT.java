package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.parsers.JsonParser;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@DisableForNative
public class CycloneDxIT extends MojoTestBase {

    /**
     * Builds a single module of a multi-module project with {@code -pl}, which leaves the parent POM
     * out of the Maven reactor while it is not installed in any repository either. The SBOM generator
     * still has to be able to resolve the effective model of the application module.
     *
     * @see <a href="https://github.com/quarkusio/quarkus/issues/57217">#57217</a>
     */
    @Test
    public void testSingleModuleOfModularProject() throws Exception {
        final File testDir = initProject("projects/cyclonedx-modular");
        final RunningInvoker running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(
                List.of("clean", "package", "-DskipTests", "-pl", "app"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");

        final File sbomFile = new File(testDir, "app/target/quarkus-run-cyclonedx.json");
        assertThat(sbomFile).exists();
        final Bom bom = new JsonParser().parse(sbomFile);

        final Component appComponent = getComponent(bom, "org.acme", "cyclonedx-modular-app");
        assertThat(appComponent.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(appComponent.getPurl())
                .isEqualTo("pkg:maven/org.acme/cyclonedx-modular-app@1.0-SNAPSHOT?type=jar");
        // the description originates from the effective model of the module, which is what used to fail to resolve
        assertThat(appComponent.getDescription()).isEqualTo("CycloneDX modular project test application");

        assertThat(getComponent(bom, "io.quarkus", "quarkus-cyclonedx")).isNotNull();
    }

    private static Component getComponent(Bom bom, String group, String name) {
        assertThat(bom.getComponents()).isNotNull();
        for (Component c : bom.getComponents()) {
            if (group.equals(c.getGroup()) && name.equals(c.getName())) {
                return c;
            }
        }
        throw new AssertionError("Failed to locate component " + group + ":" + name + " in the SBOM");
    }
}
