package io.quarkus.produi.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ProdUIPageVisibilityTest {

    @Test
    void pagesAreShownByDefault() {
        assertThat(ProdUIPageVisibility.isVisible("configuration", Map.of())).isTrue();
        assertThat(ProdUIPageVisibility.isVisible("quarkus-cache", Map.of())).isTrue();
    }

    @Test
    void explicitlyDisabledPageIsHidden() {
        assertThat(ProdUIPageVisibility.isVisible("configuration", Map.of("configuration", false))).isFalse();
    }

    @Test
    void explicitlyEnabledPageIsShown() {
        assertThat(ProdUIPageVisibility.isVisible("endpoints", Map.of("endpoints", true))).isTrue();
    }

    @Test
    void unknownPageAndNullsAreShown() {
        assertThat(ProdUIPageVisibility.isVisible("loggers", Map.of("configuration", false))).isTrue();
        assertThat(ProdUIPageVisibility.isVisible(null, Map.of("configuration", false))).isTrue();
        assertThat(ProdUIPageVisibility.isVisible("loggers", null)).isTrue();
    }

    @Test
    void hiddenExtensionNamespaceIsNotExposed() {
        assertThat(ProdUIPageVisibility.isNamespaceExposed("quarkus-cache", Map.of("quarkus-cache", false))).isFalse();
    }

    @Test
    void exposedExtensionNamespaceByDefault() {
        assertThat(ProdUIPageVisibility.isNamespaceExposed("quarkus-cache", Map.of())).isTrue();
        assertThat(ProdUIPageVisibility.isNamespaceExposed("quarkus-cache", Map.of("quarkus-cache", true))).isTrue();
    }

    @Test
    void builtInNamespaceIsAlwaysExposed() {
        // The built-in namespace is shared by several separately-gated built-in pages, so disabling one built-in page
        // (or even the namespace id itself) must never drop the shared built-in json-rpc methods.
        assertThat(ProdUIPageVisibility.isNamespaceExposed("quarkus-produi", Map.of("configuration", false))).isTrue();
        assertThat(ProdUIPageVisibility.isNamespaceExposed("quarkus-produi", Map.of("quarkus-produi", false))).isTrue();
    }
}
