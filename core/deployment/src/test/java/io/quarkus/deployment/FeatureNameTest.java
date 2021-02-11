package io.quarkus.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class FeatureNameTest {

    @Test
    public void testName() {
        assertEquals("agroal", Feature.AGROAL.getName());
        assertEquals("security-jpa", Feature.SECURITY_JPA.getName());
        assertEquals("elasticsearch-rest-client", Feature.ELASTICSEARCH_REST_CLIENT.getName());
    }

}
