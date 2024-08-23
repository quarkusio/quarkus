package io.quarkus.io.smallrye.graphql.keycloak;

import jakarta.annotation.security.RolesAllowed;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.graphql.api.Subscription;
import io.smallrye.mutiny.Multi;

@GraphQLApi
public class SecuredResource {

    // Seems to be a requirement to have a query or mutation in a GraphQLApi.
    // This is a workaround for the time being.
    @Query
    public TestResponse unusedQuery() {
        return null;
    }

    @Subscription
    @RolesAllowed("user")
    @NonBlocking
    public Multi<TestResponse> sub() {
        return Multi.createFrom().emitter(emitter -> emitter.emit(new TestResponse("Hello World")));
    }

    public static class TestResponse {

        private final String value;

        public TestResponse(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
