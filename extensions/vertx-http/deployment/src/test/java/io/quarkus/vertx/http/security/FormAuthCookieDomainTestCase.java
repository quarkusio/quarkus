package io.quarkus.vertx.http.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.ext.web.Router;

public class FormAuthCookieDomainTestCase {

    private static final String CREDENTIAL_COOKIE = "quarkus-credential";
    private static final String LOCATION_COOKIE = "quarkus-redirect-location";

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class,
                            TestTrustedIdentityProvider.class, FormAuthRoutes.class))
            .overrideConfigKey("quarkus.http.auth.form.enabled", "true")
            .overrideConfigKey("quarkus.http.auth.form.cookie-domain", "example.test")
            .overrideConfigKey("quarkus.http.auth.permission.authenticated.paths", "/admin,/logout")
            .overrideConfigKey("quarkus.http.auth.permission.authenticated.policy", "authenticated")
            .overrideConfigKey("quarkus.http.auth.session.encryption-key",
                    "CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT");

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @Test
    public void testExpiredCookiesHaveConfiguredDomainAndPath() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        // the client does not store cookies for the 'example.test' domain, therefore the cookies are sent explicitly
        ExtractableResponse<Response> response = RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/login.html"))
                .extract();
        assertThat(getSetCookieAttributes(response, LOCATION_COOKIE)).contains("Domain=example.test", "Path=/");
        String location = response.cookie(LOCATION_COOKIE);

        response = RestAssured
                .given()
                .redirects().follow(false)
                .cookie(LOCATION_COOKIE, location)
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .when()
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/admin"))
                .extract();
        assertThat(getSetCookieAttributes(response, CREDENTIAL_COOKIE)).contains("Domain=example.test", "Path=/")
                .doesNotContain("Max-Age=0");
        assertThat(getSetCookieAttributes(response, LOCATION_COOKIE)).contains("Max-Age=0", "Domain=example.test",
                "Path=/");
        String credential = response.cookie(CREDENTIAL_COOKIE);

        RestAssured
                .given()
                .redirects().follow(false)
                .cookie(CREDENTIAL_COOKIE, credential)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("admin"));

        response = RestAssured
                .given()
                .redirects().follow(false)
                .cookie(CREDENTIAL_COOKIE, credential)
                .when()
                .get("/logout")
                .then()
                .assertThat()
                .statusCode(200)
                .extract();
        assertThat(getSetCookieAttributes(response, CREDENTIAL_COOKIE)).startsWith(CREDENTIAL_COOKIE + "=")
                .contains("Max-Age=0", "Domain=example.test", "Path=/");
    }

    private static List<String> getSetCookieAttributes(ExtractableResponse<Response> response, String cookieName) {
        List<String> setCookies = response.headers().getValues("Set-Cookie").stream()
                .filter(setCookie -> setCookie.startsWith(cookieName + "="))
                .toList();
        assertThat(setCookies).hasSize(1);
        return Arrays.stream(setCookies.get(0).split(";")).map(String::trim).toList();
    }

    @ApplicationScoped
    public static class FormAuthRoutes {

        void setup(@Observes Router router) {
            router.route("/admin").handler(rc -> {
                QuarkusHttpUser user = (QuarkusHttpUser) rc.user();
                rc.response().end(user.getSecurityIdentity().getPrincipal().getName());
            });
            router.route("/logout").handler(rc -> {
                FormAuthenticationMechanism.logout(rc);
                rc.response().end();
            });
        }
    }
}
