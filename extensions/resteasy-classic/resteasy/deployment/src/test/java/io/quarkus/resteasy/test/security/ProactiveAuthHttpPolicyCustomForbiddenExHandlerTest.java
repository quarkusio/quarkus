package io.quarkus.resteasy.test.security;

import static io.quarkus.resteasy.test.security.ProactiveAuthHttpPolicyCustomForbiddenExHandlerTest.CustomForbiddenFailureHandler.CUSTOM_FORBIDDEN_EXCEPTION_HANDLER;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ProactiveAuthHttpPolicyCustomForbiddenExHandlerTest {

    private static final String PROPERTIES = "quarkus.http.auth.basic=true\n"
            + "quarkus.http.auth.policy.user-policy.roles-allowed=user\n"
            + "quarkus.http.auth.permission.roles.paths=/secured\n"
            + "quarkus.http.auth.permission.roles.policy=user-policy";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class)
                    .addAsResource(new StringAsset(PROPERTIES), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("a d m i n", "a d m i n", "a d m i n");
    }

    @Test
    public void testDeniedAccessAdminResource() {
        RestAssured.given().auth().basic("a d m i n", "a d m i n").when().get("/secured").then().statusCode(403)
                .body(equalTo(CUSTOM_FORBIDDEN_EXCEPTION_HANDLER));
    }

    @Path("/secured")
    public static class SecuredResource {

        @GET
        public String get() {
            throw new IllegalStateException();
        }

    }

    @ApplicationScoped
    public static final class CustomForbiddenFailureHandler {

        public static final String CUSTOM_FORBIDDEN_EXCEPTION_HANDLER = CustomForbiddenFailureHandler.class.getName();

        public void init(@Observes Router router) {
            router.route().failureHandler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    if (event.failure() instanceof ForbiddenException) {
                        event.response().setStatusCode(FORBIDDEN.getStatusCode())
                                .end(CUSTOM_FORBIDDEN_EXCEPTION_HANDLER);
                    } else {
                        event.next();
                    }
                }
            });
        }

    }

}
