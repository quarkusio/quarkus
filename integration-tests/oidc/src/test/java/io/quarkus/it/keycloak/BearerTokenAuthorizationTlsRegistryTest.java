package io.quarkus.it.keycloak;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@TestProfile(TlsRegistryTestProfile.class)
@QuarkusTest
@QuarkusTestResource(KeycloakXTestResourceLifecycleManager.class)
public class BearerTokenAuthorizationTlsRegistryTest extends AbstractBearerTokenAuthorizationTest {

}
