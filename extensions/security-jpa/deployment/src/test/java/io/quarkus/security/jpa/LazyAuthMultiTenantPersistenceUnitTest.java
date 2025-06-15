package io.quarkus.security.jpa;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class LazyAuthMultiTenantPersistenceUnitTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(MinimalUserEntity.class, CustomHibernateTenantResolver.class, RolesEndpointClassLevel.class)
            .addAsResource("minimal-config/import.sql", "import.sql")
            .addAsResource("multitenant-persistence-unit/application.properties", "application.properties")
            .addAsResource(new StringAsset("quarkus.http.auth.proactive=false\n"),
                    "META-INF/microprofile-config.properties"));

    @Test
    public void testRoutingCtxAccessInsideTenantResolver() {
        // RoutingContext is used and proactive auth is disabled => no issues
        CustomHibernateTenantResolver.useRoutingContext = true;
        try {
            // tenant 'one'
            RestAssured.given().auth().preemptive().basic("user", "user").queryParam("tenant", "one").when()
                    .get("/roles-class/routing-context").then().statusCode(200).body(Matchers.is("true"));
            // tenant 'two'
            RestAssured.given().auth().preemptive().basic("user", "user").queryParam("tenant", "two").when()
                    .get("/roles-class/routing-context").then().statusCode(200).body(Matchers.is("true"));
            // tenant 'unknown'
            RestAssured.given().auth().preemptive().basic("user", "user").queryParam("tenant", "unknown").when()
                    .get("/roles-class/routing-context").then().statusCode(500);
        } finally {
            CustomHibernateTenantResolver.useRoutingContext = false;
        }
    }

}
