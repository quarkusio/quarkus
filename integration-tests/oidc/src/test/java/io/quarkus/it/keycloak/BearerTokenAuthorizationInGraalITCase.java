package io.quarkus.it.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class BearerTokenAuthorizationInGraalITCase extends BearerTokenAuthorizationTest {

    DevServicesContext context;

    @Test
    public void testDevServicesProperties() {
        assertThat(context.devServicesProperties()).isEmpty();
    }
}
