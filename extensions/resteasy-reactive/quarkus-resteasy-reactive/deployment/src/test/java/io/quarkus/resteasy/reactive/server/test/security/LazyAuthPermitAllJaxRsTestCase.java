package io.quarkus.resteasy.reactive.server.test.security;

import java.util.Arrays;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class LazyAuthPermitAllJaxRsTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PermitAllResource.class, PermitAllBlockingResource.class,
                            TestIdentityProvider.class,
                            TestIdentityController.class,
                            UnsecuredSubResource.class)
                    .addAsResource(new StringAsset("quarkus.http.auth.proactive=false\n"),
                            "application.properties"));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @Test
    public void testPermitAll() {
        Arrays.asList("/permitAll/defaultSecurity", "/permitAll/sub/subMethod").forEach((path) -> {
            RestAssured.get(path).then().statusCode(200);
            RestAssured.given().auth().preemptive().basic("admin", "admin").get(path).then().statusCode(200);
            RestAssured.given().auth().preemptive().basic("admin", "wrong").get(path).then().statusCode(200);
            RestAssured.given().auth().preemptive().basic("wrong", "wrong").get(path).then().statusCode(200);
        });
    }

    @Test
    public void testPermitAllBlocking() {
        Arrays.asList("/permitAllBlocking/defaultSecurity", "/permitAllBlocking/sub/subMethod").forEach((path) -> {
            RestAssured.get(path).then().statusCode(200);
            RestAssured.given().auth().preemptive().basic("admin", "admin").get(path).then().statusCode(200);
            RestAssured.given().auth().preemptive().basic("admin", "wrong").get(path).then().statusCode(200);
            RestAssured.given().auth().preemptive().basic("wrong", "wrong").get(path).then().statusCode(200);
        });
    }
}
