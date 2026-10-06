package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.testing.BaseGradleTest.writeFile;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

final class SyntheticMavenRepository implements AutoCloseable {

    private final Path repository;
    private HttpServer server;

    SyntheticMavenRepository(Path repository) {
        this.repository = repository;
    }

    URI fileUri() {
        return repository.toUri();
    }

    URI startServer() throws IOException {
        if (server != null) {
            throw new IllegalStateException("Synthetic Maven repository server is already running");
        }
        // Retain the bound server before configuring it so close() also cleans up
        // failures from context registration or startup.
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/", this::serveRepositoryFile);
        server.start();
        return URI.create("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort() + "/");
    }

    void writeSimpleArtifact(String groupId, String artifactId, String version) throws IOException {
        writeArtifact(groupId, artifactId, version, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                </project>
                """.formatted(groupId, artifactId, version));
    }

    void writeRecursivePomClosureArtifacts(String groupId, String version) throws IOException {
        writeSimpleArtifact("org.acme", "annotation-processor", version);
        writeArtifact(groupId, "grandparent", version, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>grandparent</artifactId>
                  <version>%s</version>
                  <packaging>pom</packaging>
                  <properties>
                    <closure.version>%s</closure.version>
                  </properties>
                </project>
                """.formatted(groupId, version, version));
        writeArtifact(groupId, "parent", version, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>%s</groupId>
                    <artifactId>grandparent</artifactId>
                    <version>%s</version>
                  </parent>
                  <artifactId>parent</artifactId>
                  <version>%s</version>
                  <packaging>pom</packaging>
                </project>
                """.formatted(groupId, version, version));
        writeArtifact(groupId, "bom-parent", version, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>bom-parent</artifactId>
                  <version>%s</version>
                  <packaging>pom</packaging>
                  <properties>
                    <closure.version>%s</closure.version>
                  </properties>
                </project>
                """.formatted(groupId, version, version));
        writeArtifact(groupId, "nested-bom", version, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>%s</groupId>
                    <artifactId>bom-parent</artifactId>
                    <version>%s</version>
                  </parent>
                  <artifactId>nested-bom</artifactId>
                  <version>%s</version>
                  <packaging>pom</packaging>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>%s</groupId>
                        <artifactId>library</artifactId>
                        <version>${closure.version}</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """.formatted(groupId, version, version, groupId));
        writeArtifact(groupId, "bom", version, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>bom</artifactId>
                  <version>%s</version>
                  <packaging>pom</packaging>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>%s</groupId>
                        <artifactId>nested-bom</artifactId>
                        <version>%s</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """.formatted(groupId, version, groupId, version));
        writeArtifact(groupId, "gradle-platform", version, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>gradle-platform</artifactId>
                  <version>%s</version>
                  <packaging>pom</packaging>
                </project>
                """.formatted(groupId, version));
        writeSimpleArtifact(groupId, "gradle-platform-quarkus-platform-descriptor", version);
        writeSimpleArtifact(groupId, "gradle-platform-quarkus-platform-properties", version);
        writeFile(artifactDirectory(groupId, "gradle-platform-quarkus-platform-descriptor", version)
                .resolve("gradle-platform-quarkus-platform-descriptor-" + version + "-" + version + ".json"), "{}");
        writeFile(artifactDirectory(groupId, "gradle-platform-quarkus-platform-properties", version)
                .resolve("gradle-platform-quarkus-platform-properties-" + version + ".properties"), "");
        writeArtifact(groupId, "application", version, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>%s</groupId>
                    <artifactId>parent</artifactId>
                    <version>%s</version>
                  </parent>
                  <artifactId>application</artifactId>
                  <version>%s</version>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>%s</groupId>
                        <artifactId>bom</artifactId>
                        <version>%s</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>%s</groupId>
                      <artifactId>library</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>%s</groupId>
                      <artifactId>provided-library</artifactId>
                      <version>%s</version>
                      <scope>provided</scope>
                    </dependency>
                    <dependency>
                      <groupId>%s</groupId>
                      <artifactId>optional-library</artifactId>
                      <version>%s</version>
                      <optional>true</optional>
                    </dependency>
                    <dependency>
                      <groupId>%s</groupId>
                      <artifactId>runtime-library</artifactId>
                      <version>%s</version>
                      <scope>runtime</scope>
                    </dependency>
                  </dependencies>
                </project>
                """.formatted(groupId, version, version, groupId, version, groupId,
                groupId, version, groupId, version, groupId, version));
        writeSimpleArtifact(groupId, "library", version);
        writeSimpleArtifact(groupId, "runtime-library", version);
    }

    void writeProfileActivatedPomArtifacts(String groupId, String version) throws IOException {
        writeArtifact(groupId, "bom-one", version, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>bom-one</artifactId>
                  <version>%s</version>
                  <packaging>pom</packaging>
                </project>
                """.formatted(groupId, version));
        writeArtifact(groupId, "bom-two", version, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>bom-two</artifactId>
                  <version>%s</version>
                  <packaging>pom</packaging>
                </project>
                """.formatted(groupId, version));
        writeArtifact(groupId, "application", version, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>application</artifactId>
                  <version>%s</version>
                  <profiles>
                    <profile>
                      <id>bom-one</id>
                      <activation>
                        <property>
                          <name>closure.bom</name>
                          <value>one</value>
                        </property>
                      </activation>
                      <dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>%s</groupId>
                            <artifactId>bom-one</artifactId>
                            <version>%s</version>
                            <type>pom</type>
                            <scope>import</scope>
                          </dependency>
                        </dependencies>
                      </dependencyManagement>
                    </profile>
                    <profile>
                      <id>bom-two</id>
                      <activation>
                        <property>
                          <name>closure.bom</name>
                          <value>two</value>
                        </property>
                      </activation>
                      <dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>%s</groupId>
                            <artifactId>bom-two</artifactId>
                            <version>%s</version>
                            <type>pom</type>
                            <scope>import</scope>
                          </dependency>
                        </dependencies>
                      </dependencyManagement>
                    </profile>
                  </profiles>
                </project>
                """.formatted(groupId, version, groupId, version, groupId, version));
    }

    void writeArtifact(String groupId, String artifactId, String version, String pom) throws IOException {
        Path artifactDirectory = artifactDirectory(groupId, artifactId, version);
        Files.createDirectories(artifactDirectory);
        String baseName = artifactId + "-" + version;
        writeFile(artifactDirectory.resolve(baseName + ".pom"), pom);
        writeJar(artifactDirectory.resolve(baseName + ".jar"));
    }

    void writeJarArtifact(String groupId, String artifactId, String version) throws IOException {
        Path artifactDirectory = artifactDirectory(groupId, artifactId, version);
        Files.createDirectories(artifactDirectory);
        writeJar(artifactDirectory.resolve(artifactId + "-" + version + ".jar"));
    }

    @Override
    public void close() {
        HttpServer runningServer = server;
        server = null;
        if (runningServer != null) {
            runningServer.stop(0);
        }
    }

    private static void writeJar(Path jar) throws IOException {
        try (JarOutputStream ignored = new JarOutputStream(Files.newOutputStream(jar))) {
        }
    }

    private Path artifactDirectory(String groupId, String artifactId, String version) {
        return repository.resolve(groupId.replace('.', '/')).resolve(artifactId).resolve(version);
    }

    private void serveRepositoryFile(HttpExchange exchange) throws IOException {
        try {
            Path file = repository.resolve(exchange.getRequestURI().getPath().substring(1)).normalize();
            if (!file.startsWith(repository) || !Files.isRegularFile(file)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] content = Files.readAllBytes(file);
            boolean head = "HEAD".equals(exchange.getRequestMethod());
            exchange.sendResponseHeaders(200, head ? -1 : content.length);
            if (!head) {
                exchange.getResponseBody().write(content);
            }
        } finally {
            exchange.close();
        }
    }
}
