package io.quarkus.vertx.http.security;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class IdentityProviderTestCase {

    private static final String APP_PROPS = "" + "quarkus.http.auth.basic=true\n"
            + "quarkus.http.auth.policy.r1.roles-allowed=test\n"
            + "quarkus.http.auth.permission.roles1.paths=/secured\n"
            + "quarkus.http.auth.permission.roles1.policy=r1\n";
    private static final String AUTHENTICATION_FAILED_EXCEPTION = "AuthenticationFailedException";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, PathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @Test
    public void testAuthFailedExIsThrownIfIdentityIsNull() {
        // verify that when a matching HttpAuthenticationMechanism requests the request credentials be converted
        // to SecurityIdentity and IdentityProvider returns null then AuthenticationFailedException is thrown;
        // previously an anonymous identity was returned and request continued without exception
        RestAssured.given().auth().basic("trix", "1234").get("/secured").then().assertThat().statusCode(401)
                .body(Matchers.equalTo(AUTHENTICATION_FAILED_EXCEPTION));
    }

    @ApplicationScoped
    public static class ExceptionHandler {

        @Inject
        Router router;

        public void init(@Observes StartupEvent startupEvent) {
            router.route().failureHandler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext routingContext) {
                    if (routingContext.failure() instanceof AuthenticationFailedException) {
                        routingContext.response().end(AUTHENTICATION_FAILED_EXCEPTION);
                    } else {
                        routingContext.next();
                    }
                }
            });
        }

    }

}
