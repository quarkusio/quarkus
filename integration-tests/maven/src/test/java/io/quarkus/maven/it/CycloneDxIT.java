package io.quarkus.maven.it;

import static io.quarkus.maven.it.CycloneDxTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.parsers.JsonParser;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.devmode.util.DevModeClient;
import io.smallrye.common.process.ProcessUtil;

@DisableForNative
public class CycloneDxIT extends MojoTestBase {

    private RunningInvoker running;
    private File testDir;

    @Test
    public void testFastJar() throws Exception {
        testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-fast-jar");
        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(
                List.of("package", "-DskipTests"),
                Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final Bom bom = parseSbom(testDir, "quarkus-run-cyclonedx.json");
        assertRunnerMainComponent(bom);

        final List<Component> components = bom.getComponents();
        assertThat(components).isNotEmpty();
        assertComponent(components, "io.quarkus", "quarkus-rest", "runtime", "lib/main/");
        assertComponent(components, "io.quarkus", "quarkus-rest-deployment", "development", null);
        assertComponent(components, "io.quarkus", "quarkus-cyclonedx", "runtime", "lib/main/");
        assertComponent(components, "io.quarkus", "quarkus-cyclonedx-deployment", "development", null);
    }

    @Test
    public void testUberJar() throws Exception {
        testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-uber-jar");
        running = new RunningInvoker(testDir, false);

        Properties p = new Properties();
        p.setProperty("quarkus.package.jar.type", "uber-jar");

        final MavenProcessInvocationResult result = running.execute(
                List.of("package", "-DskipTests"),
                Map.of(), p);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final Bom bom = parseSbom(testDir, "acme-app-1.0-SNAPSHOT-runner-cyclonedx.json");

        final Component mainComponent = bom.getMetadata().getComponent();
        assertThat(mainComponent).isNotNull();
        assertThat(mainComponent.getGroup()).isEqualTo("org.acme");
        assertThat(mainComponent.getName()).isEqualTo("acme-app");
        assertThat(mainComponent.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(mainComponent.getType()).isEqualTo(Component.Type.APPLICATION);
        assertThat(mainComponent.getPurl()).isEqualTo(
                "pkg:maven/org.acme/acme-app@1.0-SNAPSHOT?classifier=runner&type=jar");
        assertComponentScope(mainComponent, "runtime");

        // uber-jar components have no evidence location
        final List<Component> components = bom.getComponents();
        assertThat(components).isNotEmpty();
        assertComponent(components, "io.quarkus", "quarkus-rest", "runtime", null);
        assertComponent(components, "io.quarkus", "quarkus-rest-deployment", "development", null);
        assertComponent(components, "io.quarkus", "quarkus-cyclonedx", "runtime", null);
        assertComponent(components, "io.quarkus", "quarkus-cyclonedx-deployment", "development", null);
    }

    @Test
    public void testMutableJar() throws Exception {
        testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-mutable-jar");
        running = new RunningInvoker(testDir, false);

        Properties p = new Properties();
        p.setProperty("quarkus.package.jar.type", "mutable-jar");

        final MavenProcessInvocationResult result = running.execute(
                List.of("package", "-DskipTests"),
                Map.of(), p);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final Bom bom = parseSbom(testDir, "quarkus-run-cyclonedx.json");
        assertRunnerMainComponent(bom);

        // mutable-jar includes deployment jars, so both runtime and development have locations
        final List<Component> components = bom.getComponents();
        assertThat(components).isNotEmpty();
        assertComponent(components, "io.quarkus", "quarkus-rest", "runtime", "lib/main/");
        assertComponent(components, "io.quarkus", "quarkus-rest-deployment", "development", "lib/deployment/");
        assertComponent(components, "io.quarkus", "quarkus-cyclonedx", "runtime", "lib/main/");
        assertComponent(components, "io.quarkus", "quarkus-cyclonedx-deployment", "development", "lib/deployment/");
    }

    @Test
    public void testEmbeddedSbomFastJar() throws Exception {
        testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-embedded-fast-jar");
        running = new RunningInvoker(testDir, false);

        Properties p = new Properties();
        p.setProperty("quarkus.cyclonedx.embedded.enabled", "true");

        final MavenProcessInvocationResult result = running.execute(
                List.of("package", "-DskipTests"),
                Map.of(), p);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        // the embedded SBOM should be in generated-bytecode.jar, compressed by default
        final Path generatedJar = testDir.toPath()
                .resolve("target/quarkus-app/quarkus/generated-bytecode.jar");
        final Bom bom = parseCompressedEmbeddedSbom(generatedJar, "META-INF/sbom/dependency.cdx.json.gz");

        assertEmbeddedSbomComponents(bom);
    }

    @Test
    public void testEmbeddedSbomUberJar() throws Exception {
        testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-embedded-uber-jar");
        running = new RunningInvoker(testDir, false);

        Properties p = new Properties();
        p.setProperty("quarkus.package.jar.type", "uber-jar");
        p.setProperty("quarkus.cyclonedx.embedded.enabled", "true");

        final MavenProcessInvocationResult result = running.execute(
                List.of("package", "-DskipTests"),
                Map.of(), p);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        // for uber-jar, the resource is embedded directly in the runner jar, compressed by default
        final Path uberJar = testDir.toPath()
                .resolve("target/acme-app-1.0-SNAPSHOT-runner.jar");
        final Bom bom = parseCompressedEmbeddedSbom(uberJar, "META-INF/sbom/dependency.cdx.json.gz");

        assertEmbeddedSbomComponents(bom);
    }

    @Test
    public void testEmbeddedSbomCustomResourceName() throws Exception {
        testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-embedded-custom-name");
        running = new RunningInvoker(testDir, false);

        final String customResourceName = "META-INF/custom-sbom.json";
        Properties p = new Properties();
        p.setProperty("quarkus.cyclonedx.embedded.enabled", "true");
        p.setProperty("quarkus.cyclonedx.embedded.resource-name", customResourceName);

        final MavenProcessInvocationResult result = running.execute(
                List.of("package", "-DskipTests"),
                Map.of(), p);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final Path generatedJar = testDir.toPath()
                .resolve("target/quarkus-app/quarkus/generated-bytecode.jar");

        // the default resource name should not exist (neither compressed nor uncompressed)
        assertNoEmbeddedResource(generatedJar, "META-INF/sbom/dependency.cdx.json");
        assertNoEmbeddedResource(generatedJar, "META-INF/sbom/dependency.cdx.json.gz");

        // the custom resource name should be compressed by default
        final Bom bom = parseCompressedEmbeddedSbom(generatedJar, customResourceName + ".gz");
        assertEmbeddedSbomComponents(bom);
    }

    @Test
    public void testEmbeddedSbomUncompressed() throws Exception {
        testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-embedded-uncompressed");
        running = new RunningInvoker(testDir, false);

        Properties p = new Properties();
        p.setProperty("quarkus.cyclonedx.embedded.enabled", "true");
        p.setProperty("quarkus.cyclonedx.embedded.compress", "false");

        final MavenProcessInvocationResult result = running.execute(
                List.of("package", "-DskipTests"),
                Map.of(), p);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final Path generatedJar = testDir.toPath()
                .resolve("target/quarkus-app/quarkus/generated-bytecode.jar");

        // with compress=false, the resource should be uncompressed JSON without .gz extension
        assertNoEmbeddedResource(generatedJar, "META-INF/sbom/dependency.cdx.json.gz");
        final Bom bom = parseEmbeddedSbom(generatedJar, "META-INF/sbom/dependency.cdx.json");
        assertEmbeddedSbomComponents(bom);
    }

    @Test
    public void testEmbeddedSbomDisabledByDefault() throws Exception {
        testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-embedded-disabled");
        running = new RunningInvoker(testDir, false);

        final MavenProcessInvocationResult result = running.execute(
                List.of("package", "-DskipTests"),
                Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        // without enabling embedded SBOM, the resource should not be present
        final Path generatedJar = testDir.toPath()
                .resolve("target/quarkus-app/quarkus/generated-bytecode.jar");
        assertNoEmbeddedResource(generatedJar, "META-INF/sbom/dependency.cdx.json");
        assertNoEmbeddedResource(generatedJar, "META-INF/sbom/dependency.cdx.json.gz");
    }

    @Test
    public void testEndpointTriggersEmbedding() throws Exception {
        testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-endpoint-triggers-embedding");
        running = new RunningInvoker(testDir, false);

        Properties p = new Properties();
        // only enable the endpoint, not embedded.enabled
        p.setProperty("quarkus.cyclonedx.endpoint.enabled", "true");

        final MavenProcessInvocationResult result = running.execute(
                List.of("package", "-DskipTests"),
                Map.of(), p);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        // enabling the endpoint should trigger SBOM embedding via the SPI, compressed by default
        final Path generatedJar = testDir.toPath()
                .resolve("target/quarkus-app/quarkus/generated-bytecode.jar");
        final Bom bom = parseCompressedEmbeddedSbom(generatedJar, "META-INF/sbom/dependency.cdx.json.gz");
        assertEmbeddedSbomComponents(bom);
    }

    @Test
    public void testEndpointServesEmbeddedSbom() throws Exception {
        testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-endpoint-serves");
        buildProjectWithEndpoint(testDir);

        Process process = launchApplication(testDir);
        try {
            assertEmbeddedSbomComponents(fetchSbomFromEndpoint());
        } finally {
            process.destroy();
        }
    }

    @Test
    public void testEndpointInDevMode() throws Exception {
        testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-endpoint-devmode");
        running = new RunningInvoker(testDir, false);

        running.execute(
                List.of("compile", "quarkus:dev",
                        "-Dquarkus.cyclonedx.endpoint.enabled=true",
                        "-Dquarkus.analytics.disabled=true"),
                Map.of());
        try {
            assertEmbeddedSbomComponents(fetchSbomFromEndpoint());
        } finally {
            running.stop();
        }
    }

    private void buildProjectWithEndpoint(File projectDir) throws Exception {
        running = new RunningInvoker(projectDir, false);

        Properties p = new Properties();
        p.setProperty("quarkus.cyclonedx.endpoint.enabled", "true");

        final MavenProcessInvocationResult result = running.execute(
                List.of("package", "-DskipTests"),
                Map.of(), p);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        running.stop();
    }

    private static Process launchApplication(File projectDir) throws Exception {
        final Path jar = projectDir.toPath().toAbsolutePath()
                .resolve("target/quarkus-app/quarkus-run.jar");
        final File output = new File(projectDir, "target/output.log");
        output.createNewFile();

        List<String> commands = new ArrayList<>();
        commands.add(ProcessUtil.pathOfJava().toString());
        commands.add("-jar");
        commands.add(jar.toString());
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(output));
        pb.redirectError(ProcessBuilder.Redirect.appendTo(output));
        return pb.start();
    }

    private static Bom fetchSbomFromEndpoint() throws Exception {
        DevModeClient client = new DevModeClient();
        await().pollDelay(1, TimeUnit.SECONDS)
                .atMost(TestUtils.getDefaultTimeout(), TimeUnit.MINUTES)
                .until(() -> client.getHttpResponse("/.well-known/sbom", 200));

        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8080/.well-known/sbom").openConnection();
        try {
            try (InputStream is = getInputStream(conn)) {
                return new JsonParser().parse(is);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static InputStream getInputStream(HttpURLConnection conn) throws IOException {
        final InputStream raw = conn.getInputStream();
        return "gzip".equals(conn.getContentEncoding())
                ? new GZIPInputStream(raw)
                : raw;
    }

    /**
     * Asserts that an embedded SBOM has the expected structure and contains
     * expected dependency components.
     */
    private static void assertEmbeddedSbomComponents(Bom bom) {
        assertThat(bom).isNotNull();
        assertThat(bom.getMetadata()).isNotNull();
        assertThat(bom.getMetadata().getComponent()).isNotNull();

        final List<Component> components = bom.getComponents();
        assertThat(components).isNotEmpty();
        // the embedded SBOM should contain application dependencies
        assertComponent(components, "io.quarkus", "quarkus-rest", "runtime", null);
        assertComponent(components, "io.quarkus", "quarkus-rest-deployment", "development", null);
        assertComponent(components, "io.quarkus", "quarkus-cyclonedx", "runtime", null);
        assertComponent(components, "io.quarkus", "quarkus-cyclonedx-deployment", "development", null);
    }
}
