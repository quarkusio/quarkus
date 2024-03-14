package io.quarkus.resteasy.reactive.server.test.security;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

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
import io.quarkus.vertx.web.Route;
import io.restassured.RestAssured;
import io.vertx.core.http.HttpServerResponse;

public class ProactiveAuthHttpPolicyForbiddenHandlerTest {

    private static final String PROPERTIES = "quarkus.http.auth.basic=true\n" +
            "quarkus.http.auth.policy.user-policy.roles-allowed=user\n" +
            "quarkus.http.auth.permission.roles.paths=/secured\n" +
            "quarkus.http.auth.permission.roles.policy=user-policy";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, CustomForbiddenFailureHandler.class)
                    .addAsResource(new StringAsset(PROPERTIES), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("a d m i n", "a d m i n", "a d m i n");
    }

    @Test
    public void testDeniedAccessAdminResource() {
        RestAssured.given()
                .auth().basic("a d m i n", "a d m i n")
                .when().get("/secured")
                .then()
                .statusCode(403)
                .body(equalTo(CustomForbiddenFailureHandler.CUSTOM_FORBIDDEN_EXCEPTION_MAPPER));
    }

    @Path("/secured")
    public static class SecuredResource {

        @GET
        public String get() {
            throw new IllegalStateException();
        }

    }

    public static final class CustomForbiddenFailureHandler {

        public static final String CUSTOM_FORBIDDEN_EXCEPTION_MAPPER = CustomForbiddenFailureHandler.class.getName();

        @Route(type = Route.HandlerType.FAILURE)
        void handle(ForbiddenException e, HttpServerResponse response) {
            response.setStatusCode(FORBIDDEN.getStatusCode()).end(CUSTOM_FORBIDDEN_EXCEPTION_MAPPER);
        }

    }

}
