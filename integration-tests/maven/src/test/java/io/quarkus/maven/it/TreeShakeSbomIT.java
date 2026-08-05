package io.quarkus.maven.it;

import static io.quarkus.maven.it.CycloneDxTestUtils.parseCompressedEmbeddedSbom;
import static io.quarkus.maven.it.CycloneDxTestUtils.parseSbom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Pedigree;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TreeShakeSbomIT extends MojoTestBase {

    private File testDir;

    @BeforeAll
    void init() throws Exception {
        testDir = initProject("projects/tree-shake-sbom");
    }

    @Test
    void testJvmSbomPedigree() throws Exception {
        RunningInvoker invoker = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = invoker.execute(
                List.of("clean", "package", "-DskipTests"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(invoker.log()).containsIgnoringCase("BUILD SUCCESS");

        Bom bom = parseSbom(new File(testDir, "app"), "quarkus-run-cyclonedx.json");
        assertPedigreeContains(bom, "org.acme", "lib", "org/acme/lib/UnusedHelper.class");
    }

    @Test
    void testEmbeddedSbomPedigree() throws Exception {
        RunningInvoker invoker = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = invoker.execute(
                List.of("clean", "package", "-DskipTests"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(invoker.log()).containsIgnoringCase("BUILD SUCCESS");

        Path generatedJar = testDir.toPath()
                .resolve("app/target/quarkus-app/quarkus/generated-bytecode.jar");
        Bom bom = parseCompressedEmbeddedSbom(generatedJar, "META-INF/sbom/dependency.cdx.json.gz");
        assertPedigreeContains(bom, "org.acme", "lib", "org/acme/lib/UnusedHelper.class");
    }

    @Test
    @EnableForNative
    void testNativeSbomPedigree() throws Exception {
        RunningInvoker invoker = new RunningInvoker(testDir, false);
        List<String> mvnArgs = TestUtils.nativeArguments("clean", "package", "-DskipTests", "-Dnative");
        MavenProcessInvocationResult result = invoker.execute(mvnArgs, Collections.emptyMap());
        await().atMost(10, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        String processLog = invoker.log();
        try {
            assertThat(processLog).containsIgnoringCase("BUILD SUCCESS");
        } catch (AssertionError ae) {
            Assumptions.assumeFalse(processLog.contains("Cannot find the `native-image"),
                    "Skipping test since native-image tool isn't available");
            throw ae;
        } finally {
            invoker.stop();
        }

        Bom bom = parseSbom(new File(testDir, "app"), "app-1.0-SNAPSHOT-runner-cyclonedx.json");
        assertPedigreeContains(bom, "org.acme", "lib", "org/acme/lib/UnusedHelper.class");
    }

    private static void assertPedigreeContains(Bom bom, String group, String name, String removedClassPath) {
        assertThat(bom).isNotNull();
        assertThat(bom.getComponents()).isNotEmpty();
        Component component = bom.getComponents().stream()
                .filter(c -> group.equals(c.getGroup()) && name.equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertThat(component)
                .as("Expected component %s:%s in SBOM", group, name)
                .isNotNull();
        Pedigree pedigree = component.getPedigree();
        assertThat(pedigree)
                .as("Pedigree of %s:%s", group, name)
                .isNotNull();
        assertThat(pedigree.getNotes())
                .as("Pedigree notes of %s:%s", group, name)
                .contains(removedClassPath);
    }
}
