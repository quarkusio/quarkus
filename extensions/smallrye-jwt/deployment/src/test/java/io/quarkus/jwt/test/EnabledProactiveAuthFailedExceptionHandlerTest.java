package io.quarkus.jwt.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.restassured.RestAssured;
import io.vertx.core.http.HttpServerResponse;

public class EnabledProactiveAuthFailedExceptionHandlerTest {

    private static final String CUSTOMIZED_RESPONSE = "AuthenticationFailedException";
    protected static final Class<?>[] classes = { JsonValuejectionEndpoint.class, TokenUtils.class,
            AuthFailedExceptionFailureHandler.class };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClasses(classes)
            .addAsResource(new StringAsset("quarkus.http.auth.proactive=true\n"), "application.properties"));

    @Test
    public void testExMapperCustomizedResponse() {
        RestAssured.given().auth().oauth2("absolute-nonsense").get("/endp/verifyInjectedIssuer").then().statusCode(401)
                .body(Matchers.equalTo(CUSTOMIZED_RESPONSE));
    }

    public static class AuthFailedExceptionFailureHandler {

        @Route(type = Route.HandlerType.FAILURE)
        void authFailedExHandler(AuthenticationFailedException e, HttpServerResponse response) {
            response.setStatusCode(401).end(CUSTOMIZED_RESPONSE);
        }

    }
}
