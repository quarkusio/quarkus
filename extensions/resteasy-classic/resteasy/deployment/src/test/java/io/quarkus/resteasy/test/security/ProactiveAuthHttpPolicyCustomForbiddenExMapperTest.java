package io.quarkus.resteasy.test.security;

import static io.quarkus.resteasy.test.security.ProactiveAuthHttpPolicyCustomForbiddenExMapperTest.CustomForbiddenExceptionMapper.CUSTOM_FORBIDDEN_EXCEPTION_MAPPER;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

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

public class ProactiveAuthHttpPolicyCustomForbiddenExMapperTest {

    private static final String PROPERTIES = "quarkus.http.auth.basic=true\n"
            + "quarkus.http.auth.policy.user-policy.roles-allowed=user\n"
            + "quarkus.http.auth.permission.roles.paths=/secured\n"
            + "quarkus.http.auth.permission.roles.policy=user-policy";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class,
                            CustomForbiddenExceptionMapper.class)
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
                .body(equalTo(CUSTOM_FORBIDDEN_EXCEPTION_MAPPER));
    }

    @Path("/secured")
    public static class SecuredResource {

        @GET
        public String get() {
            throw new IllegalStateException();
        }

    }

    @Priority(Priorities.USER)
    @Provider
    public static class CustomForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

        public static final String CUSTOM_FORBIDDEN_EXCEPTION_MAPPER = CustomForbiddenExceptionMapper.class.getName();

        @Override
        public Response toResponse(ForbiddenException e) {
            return Response.status(FORBIDDEN).entity(CUSTOM_FORBIDDEN_EXCEPTION_MAPPER).build();
        }
    }

}
