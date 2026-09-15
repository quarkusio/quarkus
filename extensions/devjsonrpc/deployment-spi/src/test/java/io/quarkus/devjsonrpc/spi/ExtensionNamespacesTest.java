package io.quarkus.devjsonrpc.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactKey;

class ExtensionNamespacesTest {

    @Test
    void conventionalDeploymentArtifact() {
        assertEquals(ArtifactKey.ga("org.acme", "hello"),
                ExtensionNamespaces.deploymentKey("org.acme:hello-deployment:1.0"));
    }

    @Test
    void unconventionalDeploymentArtifacts() {
        assertEquals(ArtifactKey.ga("org.acme", "framework"),
                ExtensionNamespaces.deploymentKey("org.acme:framework-deployment:1.0"));
        assertEquals(ArtifactKey.ga("org.acme", "hello-extension-test"),
                ExtensionNamespaces.deploymentKey("org.acme:hello-extension-test-deployment:1.0"));
        assertEquals(ArtifactKey.ga("org.acme", "hello-build"),
                ExtensionNamespaces.deploymentKey("org.acme:hello-build:jar:1.0"));
    }

    @Test
    void noCoordinates() {
        assertNull(ExtensionNamespaces.deploymentKey(null));
        assertNull(ExtensionNamespaces.deploymentKey(" "));
    }

    @Test
    void suffixStripping() {
        assertEquals("framework", ExtensionNamespaces.stripDeploymentSuffix("framework-deployment"));
        assertEquals("framework-core", ExtensionNamespaces.stripDeploymentSuffix("framework-core"));
    }
}
