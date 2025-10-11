package io.quarkus.it.keycloak;

import java.lang.reflect.Type;
import java.util.function.BiFunction;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.PropertyNamingStrategies;

import io.quarkus.resteasy.reactive.jackson.CustomSerialization;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/api/users")
public class UsersResource {

    @Inject
    SecurityIdentity keycloakSecurityContext;

    @GET
    @Path("/me")
    @CustomSerialization(ProperCaseFunction.class) // needed because otherwise SnakeCaseObjectMapperCustomizer causes the result to not be usable by Keycloak
    public User me() {
        return new User(keycloakSecurityContext);
    }

    public static class User {

        private final String userName;

        User(SecurityIdentity securityContext) {
            this.userName = securityContext.getPrincipal().getName();
        }

        public String getUserName() {
            return userName;
        }
    }

    public static class ProperCaseFunction implements BiFunction<ObjectMapper, Type, ObjectWriter> {

        @Override
        public ObjectWriter apply(ObjectMapper objectMapper, Type type) {
            return objectMapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE).writer();
        }
    }
}
