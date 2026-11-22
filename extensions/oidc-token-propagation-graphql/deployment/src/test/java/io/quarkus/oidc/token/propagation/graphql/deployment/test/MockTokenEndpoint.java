package io.quarkus.oidc.token.propagation.graphql.deployment.test;

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

import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.vertx.core.json.JsonObject;

/**
 * Token endpoint which exchanges subject tokens for the tokens configured by the tests and counts the exchanges.
 */
@Singleton
@Path("/mock-oidc/token")
public class MockTokenEndpoint {

    private final Map<String, String> responses = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> exchanges = new ConcurrentHashMap<>();

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String exchange(@FormParam("subject_token") String subjectToken) {
        exchanges.computeIfAbsent(subjectToken, t -> new AtomicInteger()).incrementAndGet();
        String response = responses.get(subjectToken);
        if (response == null) {
            throw new IllegalStateException("No response configured for the subject token " + subjectToken);
        }
        return response;
    }

    void respondWithTokenIssuedTo(String subjectToken, String userName) {
        responses.put(subjectToken, new JsonObject()
                .put("access_token", OidcWiremockTestResource.getAccessToken(userName, Set.of("user"))).encode());
    }

    void respondWithoutAccessToken(String subjectToken) {
        responses.put(subjectToken, new JsonObject().put("token_type", "Bearer").encode());
    }

    int exchangeCount(String subjectToken) {
        AtomicInteger count = exchanges.get(subjectToken);
        return count == null ? 0 : count.get();
    }
}
