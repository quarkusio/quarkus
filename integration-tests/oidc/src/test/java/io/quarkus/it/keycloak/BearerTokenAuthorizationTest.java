package io.quarkus.it.keycloak;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
@QuarkusTestResource(KeycloakXTestResourceLifecycleManager.class)
public class BearerTokenAuthorizationTest extends AbstractBearerTokenAuthorizationTest {

}
