package io.quarkus.it.keycloak;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = KeycloakLifecycleManager.class)
public class StaticTenantConfigPolicyEnforcerTest extends AbstractPolicyEnforcerTest {

    @Test
    public void testDynamicConfigNotApplied() {
        // tests that paths secured by dynamic config is public when dynamic config resolver is not applied
        assureGetPath("/api/permission/scopes/dynamic-way-denied", 200, getAccessToken("jdoe"), null);
        assureGetPath("/dynamic-permission-tenant", 200, getAccessToken("jdoe"), null);
    }
}
