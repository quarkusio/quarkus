package io.quarkus.it.keycloak;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.SubstrateTest;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTestResource(KeycloakTestResource.class)
@SubstrateTest
@Disabled("While figuring out how to have different application.properties for different tests")
public class CodeFlowInGraalITCase extends CodeFlowTest {
}
