package io.quarkus.it.keycloak;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTestResource(KeycloakTestResource.class)
@QuarkusIntegrationTest
public class PolicyEnforcerInGraalITCase extends PolicyEnforcerTest {
}
