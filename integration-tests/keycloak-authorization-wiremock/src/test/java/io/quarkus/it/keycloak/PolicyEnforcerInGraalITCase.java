package io.quarkus.it.keycloak;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.NativeImageTest;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTestResource(KeycloakTestResource.class)
@NativeImageTest
public class PolicyEnforcerInGraalITCase extends PolicyEnforcerTest {
}
