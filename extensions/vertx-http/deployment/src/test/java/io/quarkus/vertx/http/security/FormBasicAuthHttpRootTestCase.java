package io.quarkus.vertx.http.security;

import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;

public class FormBasicAuthHttpRootTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.http.root-path=/root\n" +
            "quarkus.http.auth.form.enabled=true\n" +
            "quarkus.http.auth.form.login-page=login\n" +
            "quarkus.http.auth.form.cookie-path=/root\n" +
            "quarkus.http.auth.form.error-page=error\n" +
            "quarkus.http.auth.form.landing-page=landing\n" +
            "quarkus.http.auth.policy.r1.roles-allowed=admin\n" +
            "quarkus.http.auth.permission.roles1.paths=/root/admin\n" +
            "quarkus.http.auth.permission.roles1.policy=r1\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class,
                            PathHandler.class, FormAuthEventObserver.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @Test
    public void testFormBasedAuthSuccess() {
        Assertions.assertEquals(0, FormAuthEventObserver.syncEvents.size());
        Assertions.assertEquals(0, FormAuthEventObserver.asyncEvents.size());

        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/login"))
                .cookie("quarkus-redirect-location",
                        detailedCookie().value(containsString("/root/admin")).path(equalTo("/root")));

        Assertions.assertEquals(0, FormAuthEventObserver.syncEvents.size());
        Assertions.assertEquals(0, FormAuthEventObserver.asyncEvents.size());

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/root/admin"))
                .cookie("quarkus-credential",
                        detailedCookie().value(notNullValue()).path(equalTo("/root")));

        Assertions.assertEquals(1, FormAuthEventObserver.syncEvents.size());
        var event = FormAuthEventObserver.syncEvents.get(0);
        Assertions.assertNotNull(event.getSecurityIdentity());
        Assertions.assertEquals("admin", event.getSecurityIdentity().getPrincipal().getName());
        String eventType = (String) event.getEventProperties().get(FormAuthenticationEvent.FORM_CONTEXT);
        Assertions.assertNotNull(eventType);
        Assertions.assertEquals(FormAuthenticationEvent.FormEventType.FORM_LOGIN.toString(), eventType);
        Awaitility.await().untilAsserted(() -> Assertions.assertEquals(1, FormAuthEventObserver.asyncEvents.size()));

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("admin:/root/admin"));

        Assertions.assertEquals(1, FormAuthEventObserver.syncEvents.size());
        Assertions.assertEquals(1, FormAuthEventObserver.asyncEvents.size());
    }

    public static class FormAuthEventObserver {
        private static final List<FormAuthenticationEvent> syncEvents = new CopyOnWriteArrayList<>();
        private static final List<FormAuthenticationEvent> asyncEvents = new CopyOnWriteArrayList<>();

        void observe(@Observes FormAuthenticationEvent event) {
            syncEvents.add(event);
        }

        void observeAsync(@ObservesAsync FormAuthenticationEvent event) {
            asyncEvents.add(event);
        }
    }
}
