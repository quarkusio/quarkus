package io.quarkus.security.jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EagerAuthMultiTenantPersistenceUnitTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(MinimalUserEntity.class)
                    .addClass(CustomHibernateTenantResolver.class)
                    .addAsResource("minimal-config/import.sql", "import.sql")
                    .addAsResource("multitenant-persistence-unit/application.properties", "application.properties"));

    @Test
    public void testRoutingCtxAccessInsideTenantResolver() {
        // RoutingContext is not used inside TenantResolver to resolve tenant
        RestAssured.given().auth().preemptive().basic("user", "user").when().get("/jaxrs-secured/roles-class/routing-context")
                .then().statusCode(200);

        // RoutingContext is used and proactive auth is enabled => expect error
        CustomHibernateTenantResolver.useRoutingContext = true;
        try {
            RestAssured.given().auth().preemptive().basic("user", "user").queryParam("tenant", "two").when()
                    .get("/jaxrs-secured/roles-class")
                    .then().statusCode(500);
        } finally {
            CustomHibernateTenantResolver.useRoutingContext = false;
        }
    }

}
