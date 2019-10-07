package io.quarkus.it.keycloak;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.SubstrateTest;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTestResource(KeycloakTestResource.class)
@SubstrateTest
public class BearerTokenAuthorizationInGraalITCase extends BearerTokenAuthorizationTest {
}
