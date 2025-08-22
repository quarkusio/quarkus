package io.quarkus.it.smallrye.graphql.keycloak;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Same as GraphQLAuthExpiryTest but uses deferred client authorization.
 */
@QuarkusTest
@TestProfile(GraphQLAuthExpiryDeferredTest.class)
public class GraphQLAuthExpiryDeferredTest extends GraphQLAuthExpiryTest implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.http.auth.proactive", "false");
    }
}
