package io.quarkus.jackson.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.ObjectMapper;

public class JacksonPropagateTransientMarkerDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest();

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testTransientMarkerNotPropagatedByDefault() {
        String json = objectMapper.writeValueAsString(new BeanWithTransientField("bob", "s3cr3t"));
        // By default the transient marker is not propagated, so the getter still exposes the property
        assertThat(json).contains("name").contains("token").contains("s3cr3t");
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
