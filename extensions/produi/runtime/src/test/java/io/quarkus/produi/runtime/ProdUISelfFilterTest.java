package io.quarkus.produi.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProdUISelfFilterTest {

    @Test
    void recognisesOwnLoggers() {
        assertThat(ProdUISelfFilter.isSelfLogger("io.quarkus.produi")).isTrue();
        assertThat(ProdUISelfFilter.isSelfLogger("io.quarkus.produi.runtime.advisor.AdvisorProdUIService")).isTrue();
    }

    @Test
    void doesNotMatchUnrelatedOrPrefixCollidingLoggers() {
        assertThat(ProdUISelfFilter.isSelfLogger("io.quarkus.cache")).isFalse();
        // A logger that merely starts with the same characters but is a different package must not match.
        assertThat(ProdUISelfFilter.isSelfLogger("io.quarkus.produix")).isFalse();
        assertThat(ProdUISelfFilter.isSelfLogger("org.acme.app")).isFalse();
        assertThat(ProdUISelfFilter.isSelfLogger(null)).isFalse();
    }

    @Test
    void recognisesOwnArtifacts() {
        assertThat(ProdUISelfFilter.isSelfArtifact("io.quarkus", "quarkus-produi")).isTrue();
        assertThat(ProdUISelfFilter.isSelfArtifact("io.quarkus", "quarkus-produi-deployment")).isTrue();
    }

    @Test
    void doesNotMatchOtherArtifactsOrGroups() {
        assertThat(ProdUISelfFilter.isSelfArtifact("io.quarkus", "quarkus-cache")).isFalse();
        assertThat(ProdUISelfFilter.isSelfArtifact("org.acme", "quarkus-produi")).isFalse();
        assertThat(ProdUISelfFilter.isSelfArtifact("io.quarkus", null)).isFalse();
    }
}
