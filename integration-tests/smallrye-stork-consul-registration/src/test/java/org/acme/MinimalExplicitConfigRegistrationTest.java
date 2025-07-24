package org.acme;

import static org.hamcrest.CoreMatchers.containsString;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

/**
 * A configuration block is defined for a single service (see Map returned by ConsulTestResource), but without the need to
 * indicate the registrar type.
 */

@QuarkusTest
@TestProfile(MinimalExplicitConfigRegistrationTest.MinimalExplicitConfigProfile.class)
@QuarkusTestResource(ConsulContainerWithFixedPortsTestResource.class)
public class MinimalExplicitConfigRegistrationTest {

    //    @Inject
    //    @ConfigProperty(name = "consul.host")
    //    String consulHost;
    //
    //    @Inject
    //    @ConfigProperty(name = "consul.port")
    //    String consulPort;

    public static class MinimalExplicitConfigProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "minimal";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.stork.my-service.service-registrar.ip-address", "145.123.145.145");
        }

    }

    @Test
    public void test() {
        //        String consulUrl = "http://" + consulHost + ":" + consulPort;
        //        RestAssured.get(consulUrl + "/v1/agent/service/my-service").then()
        //                .statusCode(200)
        //                .body(containsString("\"Service\": \"my-service\""));

        RestAssured.get("http://localhost:8500/v1/agent/service/my-service")
                .then()
                .statusCode(200)
                .body(containsString("\"Service\": \"my-service\""),
                        containsString("\"145.123.145.145\""));

    }

}
