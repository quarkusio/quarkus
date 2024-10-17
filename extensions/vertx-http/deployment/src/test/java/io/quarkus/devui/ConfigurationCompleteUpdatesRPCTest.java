package io.quarkus.devui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUITest;
import io.quarkus.devui.tests.JsonRPCServiceClient;
import io.quarkus.devui.tests.Namespace;
import io.quarkus.test.QuarkusDevModeTest;

@DevUITest(@Namespace("devui-configuration"))
public class ConfigurationCompleteUpdatesRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    // TODO Constructor Injection is not possible because the test instance is created by QuarkusDevModeTest
    private JsonRPCServiceClient client;

    @BeforeEach
    void setup(JsonRPCServiceClient client) {
        this.client = client;
    }

    @Test
    void testSavePropertiesAsString() throws Exception {
        final String appProperties = """
                x = y
                # test comment
                a = b
                """;
        final var response = client
                .request(
                        "updatePropertiesAsString",
                        Map.of(
                                "type", "properties",
                                "content", appProperties))
                .send()
                .get(10, TimeUnit.SECONDS);
        assertThat(response)
                .isNotNull();
        assertThat(response.asBoolean())
                .isTrue();

        final var result = client
                .request("getProjectPropertiesAsString")
                .send()
                .get(10, TimeUnit.SECONDS);
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
        final var response = client
                .request(
                        "updatePropertiesAsString",
                        Map.of(
                                "type", "json",
                                "content", appProperties))
                .send()
                .get(10, TimeUnit.SECONDS);
        assertThat(response)
                .isNotNull();
        assertThat(response.asBoolean())
                .isFalse();

    }

}
