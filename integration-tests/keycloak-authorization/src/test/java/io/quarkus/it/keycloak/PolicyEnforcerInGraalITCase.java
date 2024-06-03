package io.quarkus.it.keycloak;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusIntegrationTest
public class PolicyEnforcerInGraalITCase extends StaticTenantConfigPolicyEnforcerTest {

    @Test
    public void testPartyTokenRequest() {
        // the point is to check TokenIntrospectionToken can be deserialized
        final var response = RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/party-token-permissions-size");
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.body().as(int.class) > 0);
    }

}
