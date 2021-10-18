package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RolesAllowedLazyAuthTestCase extends AbstractRolesAllowedTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.http.auth.basic=true\n" +
            "quarkus.http.limits.max-body-size=100m\n" +
            "quarkus.http.auth.policy.r1.roles-allowed=test\n" +
            "quarkus.http.auth.policy.r2.roles-allowed=admin\n" +
            "quarkus.http.auth.permission.roles1.paths=/roles1,/deny,/permit,/combined,/wildcard1/*,/wildcard2*\n" +
            "quarkus.http.auth.permission.roles1.policy=r1\n" +
            "quarkus.http.auth.permission.roles2.paths=/roles2,/deny,/permit/combined,/wildcard3/*\n" +
            "quarkus.http.auth.permission.roles2.policy=r2\n" +
            "quarkus.http.auth.permission.permit1.paths=/permit\n" +
            "quarkus.http.auth.permission.permit1.policy=permit\n" +
            "quarkus.http.auth.permission.deny1.paths=/deny,/combined\n" +
            "quarkus.http.auth.permission.deny1.policy=deny\n" +
            "quarkus.http.auth.proactive=false\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityController.class, TestIdentityProvider.class, PathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @Test
    public void testUnauthenticatedPath() {

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/public")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo(":/public"));
    }

}
