package io.quarkus.devui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class ConfigurationCompleteUpdatesRPCTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    public ConfigurationCompleteUpdatesRPCTest() {
        super("devui-configuration");
    }

    @Test
    void testSavePropertiesAsString() throws Exception {
        final String appProperties = """
                x = y
                # test comment
                a = b
                """;
        final var response = super.executeJsonRPCMethod(
                "updatePropertiesAsString",
                Map.of(
                        "type", "properties",
                        "content", appProperties));
        assertThat(response)
                .isNotNull();
        assertThat(response.asBoolean())
                .isTrue();

        final var result = super.executeJsonRPCMethod("getProjectPropertiesAsString");
        assertThat(result.get("error"))
                .isNull();
        assertAll(
                () -> assertThat(result.get("type").asText())
                        .isEqualTo("properties"),
                () -> assertThat(result.get("value").asText().trim())
                        .isEqualTo(appProperties.trim()));
    }

    @Test
    void testSaveInvalidProperties() throws Exception {
        final String appProperties = """
                x = y
                # test comment
                a = b
                """;
        final var response = super.executeJsonRPCMethod(
                "updatePropertiesAsString",
                Map.of(
                        "type", "json",
                        "content", appProperties));
        assertThat(response)
                .isNotNull();
        assertThat(response.asBoolean())
                .isFalse();

    }

}
