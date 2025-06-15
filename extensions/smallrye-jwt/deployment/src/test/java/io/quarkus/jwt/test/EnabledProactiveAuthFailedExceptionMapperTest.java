package io.quarkus.jwt.test;

import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnabledProactiveAuthFailedExceptionMapperTest {

    private static final String CUSTOMIZED_RESPONSE = "AuthenticationFailedException";
    protected static final Class<?>[] classes = { JsonValuejectionEndpoint.class, TokenUtils.class,
            AuthFailedExceptionMapper.class };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClasses(classes)
            .addAsResource(new StringAsset("quarkus.http.auth.proactive=true\n"), "application.properties"));

    @Test
    public void testExMapperCustomizedResponse() {
        RestAssured.given().auth().oauth2("absolute-nonsense").get("/endp/verifyInjectedIssuer").then().statusCode(401)
                .body(Matchers.equalTo(CUSTOMIZED_RESPONSE));
    }

    public static class AuthFailedExceptionMapper {

        @ServerExceptionMapper(value = AuthenticationFailedException.class)
        public Response unauthorized() {
            return Response.status(401).entity(CUSTOMIZED_RESPONSE).build();
        }

    }
}
