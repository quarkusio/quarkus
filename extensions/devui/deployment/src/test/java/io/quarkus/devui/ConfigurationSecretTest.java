package io.quarkus.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import tools.jackson.databind.JsonNode;

public class ConfigurationSecretTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addAsResource(new StringAsset("""
                    quarkus.http.auth.session.encryption-key=my-secret-encryption-key-of-at-least-32-chars
                    """), "application.properties"));

    public ConfigurationSecretTest() {
        super("devui-configuration");
    }

    @Test
    void secretValuesAreMasked() throws Exception {
        JsonNode values = super.executeJsonRPCMethod("getAllValues");
        assertThat(values).isNotNull();
        assertThat(values.get("quarkus.http.auth.session.encryption-key").asText()).isEqualTo("***");
        assertThat(values.toString()).doesNotContain("my-secret-encryption-key");
    }

    @Test
    void searchConfigMasksSecretValues() throws Exception {
        JsonNode response = super.executeJsonRPCMethod("searchConfig", Map.of("query", "session.encryption-key"));
        assertThat(response).isNotNull();
        assertThat(response.toString())
                .contains("quarkus.http.auth.session.encryption-key")
                .doesNotContain("my-secret-encryption-key");
    }
}
