package io.quarkus.jwt.test;

import javax.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.junit.jupiter.api.Test;

import io.quarkus.security.AuthenticationFailedException;
import io.restassured.RestAssured;
import io.vertx.ext.web.RoutingContext;

public abstract class AbstractAuthFailedExceptionMappingTest {

    private static final String CUSTOMIZED_RESPONSE = "AuthenticationFailedException";
    protected static final Class<?>[] classes = { JsonValuejectionEndpoint.class, TokenUtils.class,
            AuthFailedExceptionMapper.class };

    @Test
    public void testExMapperCustomizedResponse() {
        RestAssured
                .given()
                .auth().oauth2("absolute-nonsense")
                .get("/endp/verifyInjectedIssuer").then()
                .statusCode(401)
                .body(Matchers.equalTo(CUSTOMIZED_RESPONSE));
    }

    public static class AuthFailedExceptionMapper {

        @ServerExceptionMapper(value = AuthenticationFailedException.class)
        public Response handle(RoutingContext routingContext) {
            return Response
                    .status(401)
                    .entity(CUSTOMIZED_RESPONSE).build();
        }

    }
}
