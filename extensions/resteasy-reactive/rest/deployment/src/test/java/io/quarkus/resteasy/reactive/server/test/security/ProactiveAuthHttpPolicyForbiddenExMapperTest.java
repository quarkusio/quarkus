package io.quarkus.resteasy.reactive.server.test.security;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
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

public class ProactiveAuthHttpPolicyForbiddenExMapperTest {

    private static final String PROPERTIES = "quarkus.http.auth.basic=true\n" +
            "quarkus.http.auth.policy.user-policy.roles-allowed=user\n" +
            "quarkus.http.auth.permission.roles.paths=/secured\n" +
            "quarkus.http.auth.permission.roles.policy=user-policy";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, CustomForbiddenExceptionMapper.class)
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
                .body(equalTo(CustomForbiddenExceptionMapper.CUSTOM_FORBIDDEN_EXCEPTION_MAPPER));
    }

    @Path("/secured")
    public static class SecuredResource {

        @GET
        public String get() {
            throw new IllegalStateException();
        }

    }

    public static final class CustomForbiddenExceptionMapper {

        public static final String CUSTOM_FORBIDDEN_EXCEPTION_MAPPER = CustomForbiddenExceptionMapper.class.getName();

        @ServerExceptionMapper(value = ForbiddenException.class)
        public Response forbidden() {
            return Response.status(FORBIDDEN).entity(CUSTOM_FORBIDDEN_EXCEPTION_MAPPER).build();
        }

    }

}
