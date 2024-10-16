package io.quarkus.jwt.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class EnabledProactiveAuthFailedExceptionMapperHttp2Test {

    private static final String CUSTOMIZED_RESPONSE = "AuthenticationFailedException";
    protected static final Class<?>[] classes = { JsonValuejectionEndpoint.class, TokenUtils.class,
            AuthFailedExceptionMapper.class };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(classes)
                    .addAsResource(new StringAsset("quarkus.http.auth.proactive=true\n" +
                            "quarkus.smallrye-jwt.blocking-authentication=true\n"), "application.properties"));

    @TestHTTPResource
    URL url;

    @Test
    public void testExMapperCustomizedResponse() throws IOException, InterruptedException, URISyntaxException {
        var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var response = client.send(
                HttpRequest.newBuilder()
                        .GET()
                        .header("Authorization", "Bearer 12345")
                        .uri(url.toURI())
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    public static class AuthFailedExceptionMapper {

        @ServerExceptionMapper(value = AuthenticationFailedException.class)
        public Response unauthorized() {
            return Response
                    .status(401)
                    .entity(CUSTOMIZED_RESPONSE).build();
        }

    }
}
