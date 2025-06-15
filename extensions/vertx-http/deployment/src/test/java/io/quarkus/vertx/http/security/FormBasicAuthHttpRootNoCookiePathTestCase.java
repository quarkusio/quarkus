package io.quarkus.vertx.http.security;

import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.function.Supplier;

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
import io.restassured.filter.cookie.CookieFilter;

public class FormBasicAuthHttpRootNoCookiePathTestCase {

    private static final String APP_PROPS = "" + "quarkus.http.root-path=/root\n"
            + "quarkus.http.auth.form.enabled=true\n" + "quarkus.http.auth.form.login-page=login\n"
            + "quarkus.http.auth.form.cookie-path=\n" + "quarkus.http.auth.form.error-page=error\n"
            + "quarkus.http.auth.form.landing-page=landing\n" + "quarkus.http.auth.policy.r1.roles-allowed=admin\n"
            + "quarkus.http.auth.permission.roles1.paths=/root/admin\n"
            + "quarkus.http.auth.permission.roles1.policy=r1\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class,
                            TestIdentityController.class, PathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("admin", "admin", "admin");
    }

    @Test
    public void testFormBasedAuthSuccess() {
        CookieFilter cookies = new CookieFilter();
        RestAssured.given().filter(cookies).redirects().follow(false).when().get("/admin").then().assertThat()
                .statusCode(302).header("location", containsString("/login")).cookie("quarkus-redirect-location",
                        detailedCookie().value(containsString("/root/admin")).path(nullValue()));

        RestAssured.given().filter(cookies).redirects().follow(false).when().formParam("j_username", "admin")
                .formParam("j_password", "admin").post("/j_security_check").then().assertThat().statusCode(302)
                .header("location", containsString("/root/admin"))
                .cookie("quarkus-credential", detailedCookie().value(notNullValue()).path(nullValue()));

        RestAssured.given().filter(cookies).redirects().follow(false).when().get("/admin").then().assertThat()
                .statusCode(200).body(equalTo("admin:/root/admin"));

    }
}
