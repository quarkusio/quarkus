package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CrossBuildDeterminismTest {

    @TempDir
    Path tempDir;

    @Test
    public void testByteIdenticalManifestsAcrossSeparateBuilds() throws IOException, InterruptedException {
        // Create two separate project directories for isolated builds
        Path project1 = tempDir.resolve("project1");
        Path project2 = tempDir.resolve("project2");

        Files.createDirectories(project1);
        Files.createDirectories(project2);

        // Set up minimal test projects
        setupTestProject(project1);
        setupTestProject(project2);

        // Output directories for manifests (directly, no kubernetes/ subdirectory)
        Path output1 = tempDir.resolve("output1");
        Path output2 = tempDir.resolve("output2");

        Files.createDirectories(output1);
        Files.createDirectories(output2);

        // Run builds independently
        runBuild(project1, output1);
        runBuild(project2, output2);

        // Both YAML and JSON must exist
        Path yaml1 = output1.resolve("kubernetes.yml");
        Path yaml2 = output2.resolve("kubernetes.yml");
        Path json1 = output1.resolve("kubernetes.json");
        Path json2 = output2.resolve("kubernetes.json");

        assertThat(yaml1).exists();
        assertThat(yaml2).exists();
        assertThat(json1).exists();
        assertThat(json2).exists();

        // Compare YAML manifests byte-for-byte
        assertThat(Files.readAllBytes(yaml1))
                .as("YAML manifests should be byte-identical across builds")
                .isEqualTo(Files.readAllBytes(yaml2));

        // Compare JSON manifests byte-for-byte
        assertThat(Files.readAllBytes(json1))
                .as("JSON manifests should be byte-identical across builds")
                .isEqualTo(Files.readAllBytes(json2));
    }

    private void setupTestProject(Path projectDir) throws IOException {
        // Create minimal project structure
        Path srcMain = projectDir.resolve("src/main/java/io/quarkus/it/kubernetes");
        Path srcResources = projectDir.resolve("src/main/resources");

        Files.createDirectories(srcMain);
        Files.createDirectories(srcResources);

        // Copy the GreetingResource class
        Path sourceRoot = Paths.get("").toAbsolutePath();
        Path greetingResource = sourceRoot.resolve("src/main/java/io/quarkus/it/kubernetes/GreetingResource.java");

        if (Files.exists(greetingResource)) {
            Files.copy(greetingResource, srcMain.resolve("GreetingResource.java"), StandardCopyOption.REPLACE_EXISTING);
        } else {
            // Create a minimal resource if source doesn't exist
            Files.writeString(srcMain.resolve("GreetingResource.java"),
                    "package io.quarkus.it.kubernetes;\n" +
                            "\n" +
                            "import jakarta.ws.rs.GET;\n" +
                            "import jakarta.ws.rs.Path;\n" +
                            "import jakarta.ws.rs.Produces;\n" +
                            "import jakarta.ws.rs.core.MediaType;\n" +
                            "\n" +
                            "@Path(\"/greeting\")\n" +
                            "public class GreetingResource {\n" +
                            "    @GET\n" +
                            "    @Produces(MediaType.TEXT_PLAIN)\n" +
                            "    public String hello() {\n" +
                            "        return \"Hello\";\n" +
                            "    }\n" +
                            "}\n");
        }

        Files.writeString(srcResources.resolve("application.properties"),
                "quarkus.kubernetes.idempotent=true\n" +
                        "quarkus.application.name=determinism-test\n" +
                        "quarkus.application.version=1.0.0\n" +
                        "quarkus.kubernetes.ports.http.container-port=8080\n" +
                        "quarkus.kubernetes.ports.https.container-port=8443\n" +
                        "quarkus.kubernetes.ports.admin.container-port=9090\n" +
                        "quarkus.kubernetes.ports.metrics.container-port=9091\n" +
                        "quarkus.kubernetes.env.configmaps=config-z,config-a,config-m\n" +
                        "quarkus.kubernetes.env.secrets=secret-z,secret-a,secret-m\n" +
                        "quarkus.kubernetes.env.vars.Z_HOST=localhost\n" +
                        "quarkus.kubernetes.env.vars.A_PORT=8080\n" +
                        "quarkus.kubernetes.env.vars.M_PROTOCOL=http\n" +
                        "quarkus.kubernetes.env.vars.B_URL=$(M_PROTOCOL)://$(Z_HOST):$(A_PORT)\n" +
                        "quarkus.kubernetes.env.vars.C_ENDPOINT=$(B_URL)/api\n" +
                        "quarkus.kubernetes.env.vars.APP_NAME=determinism-test\n" +
                        "quarkus.kubernetes.env.vars.APP_ENV=production\n");

        // Create minimal pom.xml
        String quarkusVersion = System.getProperty("project.version", "999-SNAPSHOT");
        Files.writeString(projectDir.resolve("pom.xml"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                        +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>io.quarkus.it</groupId>\n" +
                        "    <artifactId>determinism-test</artifactId>\n" +
                        "    <version>1.0.0</version>\n" +
                        "    <properties>\n" +
                        "        <maven.compiler.source>17</maven.compiler.source>\n" +
                        "        <maven.compiler.target>17</maven.compiler.target>\n" +
                        "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
                        "        <quarkus.version>" + quarkusVersion + "</quarkus.version>\n" +
                        "    </properties>\n" +
                        "    <dependencyManagement>\n" +
                        "        <dependencies>\n" +
                        "            <dependency>\n" +
                        "                <groupId>io.quarkus</groupId>\n" +
                        "                <artifactId>quarkus-bom</artifactId>\n" +
                        "                <version>${quarkus.version}</version>\n" +
                        "                <type>pom</type>\n" +
                        "                <scope>import</scope>\n" +
                        "            </dependency>\n" +
                        "        </dependencies>\n" +
                        "    </dependencyManagement>\n" +
                        "    <dependencies>\n" +
                        "        <dependency>\n" +
                        "            <groupId>io.quarkus</groupId>\n" +
                        "            <artifactId>quarkus-resteasy</artifactId>\n" +
                        "        </dependency>\n" +
                        "        <dependency>\n" +
                        "            <groupId>io.quarkus</groupId>\n" +
                        "            <artifactId>quarkus-kubernetes</artifactId>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>\n" +
                        "    <build>\n" +
                        "        <plugins>\n" +
                        "            <plugin>\n" +
                        "                <groupId>io.quarkus</groupId>\n" +
                        "                <artifactId>quarkus-maven-plugin</artifactId>\n" +
                        "                <version>${quarkus.version}</version>\n" +
                        "                <executions>\n" +
                        "                    <execution>\n" +
                        "                        <goals>\n" +
                        "                            <goal>build</goal>\n" +
                        "                        </goals>\n" +
                        "                    </execution>\n" +
                        "                </executions>\n" +
                        "            </plugin>\n" +
                        "        </plugins>\n" +
                        "    </build>\n" +
                        "</project>\n");
    }

    private void runBuild(Path projectDir, Path outputDir) throws IOException, InterruptedException {
        // Find Maven wrapper at repository root
        Path repoRoot = Paths.get("").toAbsolutePath();
        while (repoRoot != null && !Files.exists(repoRoot.resolve("mvnw"))) {
            repoRoot = repoRoot.getParent();
        }

        if (repoRoot == null) {
            throw new IllegalStateException("Could not find Maven wrapper (mvnw) in parent directories");
        }

        String mvnCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvnw.cmd" : "mvnw";
        Path mvnPath = repoRoot.resolve(mvnCmd);

        ProcessBuilder pb = new ProcessBuilder(
                mvnPath.toString(),
                "package",
                "-DskipTests",
                "-Dquarkus.kubernetes.output-directory=" + outputDir.toAbsolutePath());

        pb.directory(projectDir.toFile());
        pb.redirectErrorStream(true);

        Path buildLog = outputDir.resolve("build.log");
        pb.redirectOutput(ProcessBuilder.Redirect.to(buildLog.toFile()));

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String log = Files.readString(buildLog);
            throw new RuntimeException("Build failed with exit code " + exitCode + ". Log:\n" + log);
        }
    }
}
