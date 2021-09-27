package io.quarkus.elytron.security.ldap;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.elytron.security.ldap.rest.SingleRoleSecuredServlet;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.ldap.LdapServerTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(LdapServerTestResource.class)
public class SearchRecursiveTest {

    protected static Class[] testClasses = {
            SingleRoleSecuredServlet.class
    };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsResource("search-recursive/application.properties", "application.properties"));

    @Test()
    public void testNotSearchingRecursiveFailure() {
        RestAssured.given().auth().preemptive().basic("subUser", "subUserPassword")
                .when().get("/servlet-secured").then()
                .statusCode(200);
    }
}
