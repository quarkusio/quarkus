package io.quarkus.produi.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the JSON emission helpers behind the enriched extension cards
 * (icon, description, status and live-badge labels) in {@code produi-pages-data.js}.
 */
class ProdUIPagesDataTest {

    @Test
    void appendOptionalSkipsNullAndBlank() {
        StringBuilder sb = new StringBuilder();
        ProdUIProcessor.appendOptional(sb, "icon", null);
        ProdUIProcessor.appendOptional(sb, "icon", "");
        ProdUIProcessor.appendOptional(sb, "icon", "   ");
        assertThat(sb).isEmpty();
    }

    @Test
    void appendOptionalWritesCommaPrefixedPair() {
        StringBuilder sb = new StringBuilder();
        ProdUIProcessor.appendOptional(sb, "icon", "font-awesome-solid:database");
        assertThat(sb).hasToString(",\"icon\":\"font-awesome-solid:database\"");
    }

    @Test
    void appendOptionalEscapesValue() {
        StringBuilder sb = new StringBuilder();
        ProdUIProcessor.appendOptional(sb, "description", "a \"quoted\" back\\slash");
        assertThat(sb).hasToString(",\"description\":\"a \\\"quoted\\\" back\\\\slash\"");
    }

    @Test
    void appendFieldTracksWhetherAnythingWasWritten() {
        StringBuilder sb = new StringBuilder();
        // first field: no leading comma, returns true
        boolean wrote = ProdUIProcessor.appendField(sb, "title", "Total", false);
        assertThat(wrote).isTrue();
        // second field: comma-prefixed because something preceded it
        wrote = ProdUIProcessor.appendField(sb, "staticText", "5", true) || wrote;
        assertThat(wrote).isTrue();
        assertThat(sb).hasToString("\"title\":\"Total\",\"staticText\":\"5\"");
    }

    @Test
    void appendFieldReturnsFalseAndWritesNothingForBlank() {
        StringBuilder sb = new StringBuilder();
        boolean wrote = ProdUIProcessor.appendField(sb, "icon", null, false);
        assertThat(wrote).isFalse();
        assertThat(sb).isEmpty();
    }

    @Test
    void appendFieldDoesNotEmitLeadingCommaForFirstField() {
        StringBuilder sb = new StringBuilder();
        // Simulate: icon (blank, skipped) then title as the first real field.
        boolean wrote = ProdUIProcessor.appendField(sb, "icon", "", false);
        ProdUIProcessor.appendField(sb, "title", "Caches", wrote);
        assertThat(sb).hasToString("\"title\":\"Caches\"");
    }

    @Test
    void collectionToStringHandlesStringListAndNull() {
        assertThat(ProdUIProcessor.collectionToString(null)).isNull();
        assertThat(ProdUIProcessor.collectionToString("stable")).isEqualTo("stable");
        assertThat(ProdUIProcessor.collectionToString(List.of("preview", "experimental")))
                .isEqualTo("preview, experimental");
    }

    @Test
    void toStringListHandlesScalarListAndNull() {
        assertThat(ProdUIProcessor.toStringList(null)).isNull();
        assertThat(ProdUIProcessor.toStringList("quarkus.cache.")).containsExactly("quarkus.cache.");
        assertThat(ProdUIProcessor.toStringList(List.of("quarkus.kafka.", "kafka.", "mp.messaging.")))
                .containsExactly("quarkus.kafka.", "kafka.", "mp.messaging.");
    }

    @Test
    void appendStringArraySkipsNullAndEmpty() {
        StringBuilder sb = new StringBuilder();
        ProdUIProcessor.appendStringArray(sb, "configPrefixes", null);
        ProdUIProcessor.appendStringArray(sb, "configPrefixes", List.of());
        assertThat(sb).isEmpty();
    }

    @Test
    void appendStringArrayWritesCommaPrefixedJsonArray() {
        StringBuilder sb = new StringBuilder();
        ProdUIProcessor.appendStringArray(sb, "configPrefixes", List.of("quarkus.cache.", "a\"b"));
        assertThat(sb).hasToString(",\"configPrefixes\":[\"quarkus.cache.\",\"a\\\"b\"]");
    }

    @Test
    void escapeJsonHandlesNullAndSpecialCharacters() {
        assertThat(ProdUIProcessor.escapeJson(null)).isEmpty();
        assertThat(ProdUIProcessor.escapeJson("plain")).isEqualTo("plain");
        assertThat(ProdUIProcessor.escapeJson("he said \"hi\"")).isEqualTo("he said \\\"hi\\\"");
    }
}
