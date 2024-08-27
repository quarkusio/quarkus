package io.quarkus.elytron.security.ldap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.elytron.security.ldap.rest.SingleRoleSecuredServlet;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.ldap.LdapServerTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(LdapServerTestResource.class)
public class CacheTest {
    protected static Class[] testClasses = {
            SingleRoleSecuredServlet.class
    };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("cache/application.properties", "application.properties"));

    @Test()
    public void testNoCacheFailure() {
        RestAssured.given().auth().preemptive().basic("standardUser", "standardUserPassword")
                .when().get("/servlet-secured").then()
                .statusCode(200);
    }
}
