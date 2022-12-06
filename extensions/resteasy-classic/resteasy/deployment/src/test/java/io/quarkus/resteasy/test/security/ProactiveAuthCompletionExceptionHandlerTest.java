package io.quarkus.resteasy.test.security;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ProactiveAuthCompletionExceptionHandlerTest {

    private static final String AUTHENTICATION_COMPLETION_EX = "AuthenticationCompletionException";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class,
                            CustomAuthCompletionExceptionHandler.class)
                    .addAsResource(new StringAsset("quarkus.http.auth.form.enabled=true\n"), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("a d m i n", "a d m i n", "a d m i n");
    }

    @Test
    public void testAuthCompletionExMapper() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured
                .given()
                .filter(new CookieFilter())
                .redirects().follow(false)
                .when()
                .formParam("j_username", "a d m i n")
                .formParam("j_password", "a d m i n")
                .cookie("quarkus-redirect-location", "https://quarkus.io/guides")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(401)
                .body(Matchers.equalTo(AUTHENTICATION_COMPLETION_EX));
    }

    /**
     * Use failure handler as when proactive security is enabled, JAX-RS exception mappers won't do.
     */
    @ApplicationScoped
    public static final class CustomAuthCompletionExceptionHandler {

        public void init(@Observes Router router) {
            router.route().failureHandler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    if (event.failure() instanceof AuthenticationCompletionException) {
                        event.response().setStatusCode(401).end(AUTHENTICATION_COMPLETION_EX);
                    } else {
                        event.next();
                    }
                }
            });
        }

    }

}
