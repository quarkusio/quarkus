package io.quarkus.bootstrap.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;

class PlatformImportsDefaultCapabilityProvidersTest {

    @Test
    void parsesDefaultProviders() {
        var pi = new PlatformImportsImpl();
        pi.setPlatformProperties(Map.of(
                "platform.default-capability-provider.io.quarkus.rest",
                "io.quarkus:quarkus-rest::jar:3.8.0",
                "platform.default-capability-provider.io.quarkus.security",
                "io.quarkus:quarkus-security::jar:3.8.0",
                "platform.quarkus.native.builder-image", "some-image"));

        var providers = pi.getDefaultCapabilityProviders();
        assertThat(providers).hasSize(2);
        assertThat(providers.get("io.quarkus.rest")).isEqualTo(
                ArtifactCoords.fromString("io.quarkus:quarkus-rest::jar:3.8.0"));
        assertThat(providers.get("io.quarkus.security")).isEqualTo(
                ArtifactCoords.fromString("io.quarkus:quarkus-security::jar:3.8.0"));
    }

    @Test
    void returnsEmptyWhenNoProviders() {
        var pi = new PlatformImportsImpl();
        pi.setPlatformProperties(Map.of("platform.quarkus.native.builder-image", "img"));
        assertThat(pi.getDefaultCapabilityProviders()).isEmpty();
    }

    @Test
    void cachedResult() {
        var pi = new PlatformImportsImpl();
        pi.setPlatformProperties(Map.of(
                "platform.default-capability-provider.cap.a", "org.acme:a::jar:1.0"));
        var first = pi.getDefaultCapabilityProviders();
        var second = pi.getDefaultCapabilityProviders();
        assertThat(first).isSameAs(second);
    }
}
