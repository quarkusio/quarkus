package io.quarkus.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.testrunner.HelloResource;
import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

/**
 * Verifies that the workspace JSON-RPC operations only act on files inside the project root and
 * reject client supplied {@code file://} paths that resolve outside of it (path traversal /
 * arbitrary file read and write).
 */
public class WorkspacePathConfinementTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(HelloResource.class)
                    .add(new StringAsset("quarkus.application.name=workspace-confinement-test\n"),
                            "application.properties"));

    private static final String SECRET = "top-secret-content";

    public WorkspacePathConfinementTest() {
        super("devui-workspace");
    }

    @Test
    public void testCanReadItemInsideProjectRoot() throws Exception {
        JsonNode firstItem = firstWorkspaceItem();
        String pathUri = firstItem.get("path").asText();

        JsonNode content = super.executeJsonRPCMethod("getWorkspaceItemContent", Map.of("path", pathUri));

        assertThat(content).isNotNull();
        assertThat(content.has("content")).isTrue();
    }

    @Test
    public void testCannotReadFileOutsideProjectRoot() throws Exception {
        Path projectRoot = projectRoot();
        Path secret = projectRoot.getParent().resolve("devui-confinement-secret.txt");
        Files.writeString(secret, SECRET);
        try {
            // Absolute path pointing outside the project root
            JsonNode byAbsolute = super.executeJsonRPCMethod("getWorkspaceItemContent",
                    Map.of("path", secret.toUri().toString()));
            assertThat(byAbsolute.get("content").asText())
                    .as("Reading a file outside the project root must not leak its content").doesNotContain(SECRET);

            // The same file reached via a '..' traversal from inside the root
            String traversalUri = projectRoot.toUri().toString() + "../devui-confinement-secret.txt";
            JsonNode byTraversal = super.executeJsonRPCMethod("getWorkspaceItemContent",
                    Map.of("path", traversalUri));
            assertThat(byTraversal.get("content").asText())
                    .as("Reading via a '..' traversal must not leak its content").doesNotContain(SECRET);
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    public void testCannotWriteFileOutsideProjectRoot() throws Exception {
        Path projectRoot = projectRoot();
        Path escaped = projectRoot.getParent().resolve("devui-confinement-written.txt");
        Files.deleteIfExists(escaped);
        try {
            JsonNode result = super.executeJsonRPCMethod("saveWorkspaceItemContent",
                    Map.of("path", escaped.toUri().toString(), "content", "should-not-be-written"));

            assertThat(result).isNotNull();
            assertThat(result.get("success").asBoolean())
                    .as("Writing a file outside the project root must be rejected").isFalse();
            assertThat(Files.exists(escaped)).as("No file must be created outside the project root").isFalse();
        } finally {
            Files.deleteIfExists(escaped);
        }
    }

    @Test
    public void testCannotReadFileViaSymlinkOutsideProjectRoot() throws Exception {
        Path projectRoot = projectRoot();
        Path secret = projectRoot.getParent().resolve("devui-confinement-symlink-secret.txt");
        Files.writeString(secret, SECRET);

        Path link = projectRoot.resolve("devui-confinement-escape-link");
        Files.deleteIfExists(link);
        try {
            Files.createSymbolicLink(link, secret.getParent());
        } catch (UnsupportedOperationException | IOException e) {
            // Filesystem does not support symlinks (e.g. Windows without privileges): nothing to test
            Assumptions.assumeTrue(false, "Symlinks are not supported on this filesystem");
        }
        try {
            // A symlink inside the root that points outside of it must not be usable to escape
            String symlinkUri = link.toUri().toString() + "devui-confinement-symlink-secret.txt";
            JsonNode content = super.executeJsonRPCMethod("getWorkspaceItemContent",
                    Map.of("path", symlinkUri));
            assertThat(content.get("content").asText())
                    .as("Reading through a symlink that escapes the project root must not leak content")
                    .doesNotContain(SECRET);
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(secret);
        }
    }

    private JsonNode firstWorkspaceItem() throws Exception {
        JsonNode workspace = super.executeJsonRPCMethod("getWorkspaceItems");
        assertThat(workspace).isNotNull();
        JsonNode items = workspace.get("items");
        assertThat(items).isNotNull();
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isGreaterThan(0);
        return items.get(0);
    }

    /**
     * Derive the project root from a workspace item: its absolute path minus the relative name.
     */
    private Path projectRoot() throws Exception {
        JsonNode item = firstWorkspaceItem();
        Path abs = Paths.get(URI.create(item.get("path").asText()));
        Path relative = Paths.get(item.get("name").asText());
        Path root = abs;
        for (int i = 0; i < relative.getNameCount(); i++) {
            root = root.getParent();
        }
        return root;
    }
}
