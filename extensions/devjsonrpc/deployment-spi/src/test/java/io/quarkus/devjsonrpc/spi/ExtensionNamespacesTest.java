package io.quarkus.devjsonrpc.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

public class ExtensionNamespacesTest {

    @TempDir
    Path tempDir;

    @Test
    public void mapsAnUnconventionallyNamedDeploymentArtifactToItsRuntimeArtifact() throws IOException {
        ApplicationModel model = model(
                runtimeExtension("org.acme", "framework-core", "org.acme:framework-deployment:1.0"),
                runtimeExtension("io.quarkus", "quarkus-vertx-http", "io.quarkus:quarkus-vertx-http-deployment:999"));

        Map<ArtifactKey, String> mapping = ExtensionNamespaces.deploymentToRuntime(model);

        assertEquals("framework-core", mapping.get(ArtifactKey.ga("org.acme", "framework")));
        assertEquals("quarkus-vertx-http", mapping.get(ArtifactKey.ga("io.quarkus", "quarkus-vertx-http")));
    }

    @Test
    public void ignoresARuntimeArtifactWithoutADescriptor() throws IOException {
        ApplicationModel model = model(
                runtimeExtension("org.acme", "no-descriptor", null),
                runtimeExtension("org.acme", "framework-core", "org.acme:framework-deployment:1.0"));

        Map<ArtifactKey, String> mapping = ExtensionNamespaces.deploymentToRuntime(model);

        assertEquals(1, mapping.size(), mapping::toString);
        assertEquals("framework-core", mapping.get(ArtifactKey.ga("org.acme", "framework")));
    }

    @Test
    public void keepsTheFirstRuntimeArtifactWhenTwoClaimTheSameDeploymentArtifact() throws IOException {
        ApplicationModel model = model(
                runtimeExtension("org.acme", "first", "org.acme:shared-deployment:1.0"),
                runtimeExtension("org.acme", "second", "org.acme:shared-deployment:1.0"));

        Map<ArtifactKey, String> mapping = ExtensionNamespaces.deploymentToRuntime(model);

        assertEquals(1, mapping.size(), mapping::toString);
        assertTrue(Map.of("first", "first", "second", "second")
                .containsKey(mapping.get(ArtifactKey.ga("org.acme", "shared"))), mapping::toString);
    }

    @Test
    public void readsTheDeploymentKeyOfAnExtensionDescriptor() {
        assertEquals(ArtifactKey.ga("org.acme", "framework"),
                ExtensionNamespaces.deploymentKey("org.acme:framework-deployment:1.0"));
        assertEquals(ArtifactKey.ga("org.acme", "framework-core"),
                ExtensionNamespaces.deploymentKey("org.acme:framework-core:1.0"));
        assertNull(ExtensionNamespaces.deploymentKey(null));
        assertNull(ExtensionNamespaces.deploymentKey("  "));
    }

    @Test
    public void stripsOnlyATrailingDeploymentSuffix() {
        assertEquals("framework", ExtensionNamespaces.stripDeploymentSuffix("framework-deployment"));
        assertEquals("framework-core", ExtensionNamespaces.stripDeploymentSuffix("framework-core"));
        assertEquals("deployment-tools", ExtensionNamespaces.stripDeploymentSuffix("deployment-tools"));
    }

    private ApplicationModel model(ResolvedDependencyBuilder... extensions) {
        ApplicationModelBuilder builder = new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme")
                        .setArtifactId("app")
                        .setVersion("1.0")
                        .setResolvedPath(tempDir));
        for (ResolvedDependencyBuilder extension : extensions) {
            builder.addDependency(extension);
        }
        return builder.build();
    }

    /**
     * A runtime extension artifact resolved to a directory, carrying an extension descriptor that names the given
     * deployment artifact, or no descriptor at all when the coordinates are {@code null}.
     */
    private ResolvedDependencyBuilder runtimeExtension(String groupId, String artifactId, String deploymentCoords)
            throws IOException {
        Path root = Files.createDirectories(tempDir.resolve(groupId + "." + artifactId));
        if (deploymentCoords != null) {
            Path descriptor = root.resolve(BootstrapConstants.DESCRIPTOR_PATH);
            Files.createDirectories(descriptor.getParent());
            try (Writer writer = Files.newBufferedWriter(descriptor)) {
                writer.write(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT + "=" + deploymentCoords.replace(":", "\\:"));
            }
        }
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId(groupId)
                .setArtifactId(artifactId)
                .setVersion("1.0")
                .setResolvedPath(root)
                .setFlags(DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
    }
}
