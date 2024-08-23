package io.quarkus.elytron.security.ldap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.elytron.security.ldap.rest.SingleRoleSecuredServlet;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.ldap.LdapServerTestResource;
import io.restassured.RestAssured;

@WithTestResource(value = LdapServerTestResource.class, restrictToAnnotatedClass = false)
public class SearchRecursiveTest {

    protected static Class[] testClasses = {
            SingleRoleSecuredServlet.class
    };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("search-recursive/application.properties", "application.properties"));

    @Test()
    public void testNotSearchingRecursiveFailure() {
        RestAssured.given().auth().preemptive().basic("subUser", "subUserPassword")
                .when().get("/servlet-secured").then()
                .statusCode(200);
    }
}
