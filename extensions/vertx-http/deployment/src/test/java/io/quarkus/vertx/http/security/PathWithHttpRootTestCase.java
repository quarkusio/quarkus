package io.quarkus.vertx.http.security;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class PathWithHttpRootTestCase {

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("test", "test", "test");
    }

    private static final String APP_PROPS = "" + "# Add your application.properties here, if applicable.\n"
            + "quarkus.http.root-path=/root\n" + "quarkus.http.auth.permission.authenticated.paths=admin\n"
            + "quarkus.http.auth.permission.authenticated.policy=authenticated\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityController.class, TestIdentityProvider.class, AdminPathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @Test
    public void testAdminPath() {

        RestAssured.given().when().get("/admin").then().assertThat().statusCode(401);
        RestAssured.given().auth().preemptive().basic("test", "test").when().get("/admin").then().assertThat()
                .statusCode(200).body(Matchers.equalTo("test:/root/admin"));

    }

}
