package io.quarkus.it.keycloak;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.NativeImageTest;

@QuarkusTestResource(KeycloakTestResource.class)
@NativeImageTest
public class BearerTokenAuthorizationInGraalITCase extends BearerTokenAuthorizationTest {
}
