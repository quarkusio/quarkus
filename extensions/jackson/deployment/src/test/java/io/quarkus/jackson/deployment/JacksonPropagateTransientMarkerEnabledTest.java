package io.quarkus.jackson.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.ObjectMapper;

public class JacksonPropagateTransientMarkerEnabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.jackson.propagate-transient-marker", "true");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testTransientMarkerPropagated() {
        String json = objectMapper.writeValueAsString(new BeanWithTransientField("bob", "s3cr3t"));
        // The transient marker is propagated to the whole property, so the getter is ignored too
        assertThat(json).contains("name").doesNotContain("token").doesNotContain("s3cr3t");
    }

    public static class BeanWithTransientField {
        public String name;
        public transient String token;

        public BeanWithTransientField() {
        }

        public BeanWithTransientField(String name, String token) {
            this.name = name;
            this.token = token;
        }

        public String getToken() {
            return token;
        }
    }
}
