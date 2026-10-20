package io.quarkus.oidc.token.propagation.reactive.deployment.test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.vertx.core.json.JsonObject;

/**
 * Token endpoint which exchanges subject tokens for the tokens configured by the tests and counts the exchanges.
 */
@Singleton
@Path("/mock-oidc/token")
public class MockTokenEndpoint {

    private record TokenResponse(int status, String body) {
    }

    private final Map<String, TokenResponse> responses = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> exchanges = new ConcurrentHashMap<>();

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response exchange(@FormParam("subject_token") String subjectToken) {
        exchanges.computeIfAbsent(subjectToken, t -> new AtomicInteger()).incrementAndGet();
        TokenResponse response = responses.get(subjectToken);
        if (response == null) {
            throw new IllegalStateException("No response configured for the subject token " + subjectToken);
        }
        return Response.status(response.status()).entity(response.body()).build();
    }

    void respondWithTokenIssuedTo(String subjectToken, String userName) {
        responses.put(subjectToken, new TokenResponse(200, tokenJson(userName).encode()));
    }

    void respondWithTokenIssuedTo(String subjectToken, String userName, int expiresInSeconds) {
        responses.put(subjectToken,
                new TokenResponse(200, tokenJson(userName).put("expires_in", expiresInSeconds).encode()));
    }

    void respondWithError(String subjectToken) {
        responses.put(subjectToken, new TokenResponse(400, "{\"error\":\"invalid_grant\"}"));
    }

    int exchangeCount(String subjectToken) {
        AtomicInteger count = exchanges.get(subjectToken);
        return count == null ? 0 : count.get();
    }

    private static JsonObject tokenJson(String userName) {
        return new JsonObject().put("access_token", OidcWiremockTestResource.getAccessToken(userName, Set.of("user")));
    }
}
