package io.quarkus.deployment.builditem;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.deployment.BootstrapConfig.IncompatibleExtensions;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

public class AppModelProviderBuildItemTest {

    @TempDir
    Path tempDir;

    @Test
    public void testParseRequiresQuarkusCore() {
        assertEquals("[3.38,3.40)", AppModelProviderBuildItem.parseRequiresQuarkusCore("---\n"
                + "artifact: \"org.acme:acme-extension:1.0\"\n"
                + "metadata:\n"
                + "  built-with-quarkus-core: \"3.38.0\"\n"
                + "  requires-quarkus-core: \"[3.38,3.40)\"\n"));
        assertEquals("[3.38,)", AppModelProviderBuildItem.parseRequiresQuarkusCore("requires-quarkus-core: [3.38,)\n"));
        assertNull(AppModelProviderBuildItem.parseRequiresQuarkusCore("built-with-quarkus-core: \"3.38.0\"\n"));
    }

    @Test
    public void testIncompatibleExtension() throws IOException {
        final AppModelProviderBuildItem item = new AppModelProviderBuildItem(model("[3.38,3.40)"));

        /* Quarkus core version below the declared range */
        final RuntimeException error = assertThrows(RuntimeException.class,
                () -> item.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "3.36.1"));
        assertTrue(error.getMessage().contains("org.acme:acme-extension:1.0"), error.getMessage());
        assertTrue(error.getMessage().contains("requires Quarkus core [3.38,3.40)"), error.getMessage());

        /* Quarkus core version above the declared range */
        assertThrows(RuntimeException.class,
                () -> item.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "3.40.0"));

        /* WARN and IGNORE do not throw */
        assertDoesNotThrow(() -> item.validateExtensionCompatibility(IncompatibleExtensions.WARN, "3.36.1"));
        assertDoesNotThrow(() -> item.validateExtensionCompatibility(IncompatibleExtensions.IGNORE, "3.36.1"));
    }

    @Test
    public void testCompatibleExtension() throws IOException {
        final AppModelProviderBuildItem item = new AppModelProviderBuildItem(model("[3.38,3.40)"));
        assertDoesNotThrow(() -> item.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "3.38.1"));
        assertDoesNotThrow(() -> item.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "3.39.5"));

        final AppModelProviderBuildItem openEnded = new AppModelProviderBuildItem(model("[3.38,)"));
        assertDoesNotThrow(() -> openEnded.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "4.0.0"));
    }

    @Test
    public void testMainBranchVersionSkipsCheck() throws IOException {
        final AppModelProviderBuildItem item = new AppModelProviderBuildItem(model("[3.38,3.40)"));
        assertDoesNotThrow(() -> item.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "999-SNAPSHOT"));
    }

    @Test
    public void testNoRangeSkipsCheck() throws IOException {
        /* A plain version instead of a range is not enforced */
        final AppModelProviderBuildItem item = new AppModelProviderBuildItem(model("3.38.0"));
        assertDoesNotThrow(() -> item.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "3.36.1"));
    }

    private ApplicationModel model(String requiresQuarkusCore) throws IOException {
        final Path extensionDir = Files.createDirectories(tempDir.resolve("acme-extension/META-INF")).getParent();
        Files.writeString(extensionDir.resolve("META-INF/quarkus-extension.yaml"), "---\n"
                + "artifact: \"org.acme:acme-extension:1.0\"\n"
                + "metadata:\n"
                + "  requires-quarkus-core: \"" + requiresQuarkusCore + "\"\n");
        final Path plainJarDir = Files.createDirectories(tempDir.resolve("plain-jar"));
        return new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme")
                        .setArtifactId("acme-app")
                        .setVersion("1.0")
                        .setResolvedPath(tempDir.resolve("acme-app")))
                .addDependency(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme")
                        .setArtifactId("acme-extension")
                        .setVersion("1.0")
                        .setResolvedPath(extensionDir)
                        .setFlags(DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP
                                | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT))
                .addDependency(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme")
                        .setArtifactId("plain-jar")
                        .setVersion("1.0")
                        .setResolvedPath(plainJarDir)
                        .setFlags(DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP))
                .build();
    }
}
