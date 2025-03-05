package io.quarkus.resteasy.reactive.server.test.security;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.junit.jupiter.api.Test;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Tests internal server errors and other custom exceptions raised during
 * proactive authentication can be handled by the exception mappers.
 * For lazy authentication, it is important that these exceptions raised during authentication
 * required by HTTP permissions are also propagated.
 */
public abstract class AbstractCustomExceptionMapperTest {

    @Test
    public void testNoExceptions() {
        RestAssured.given()
                .auth().preemptive().basic("gaston", "gaston-password")
                .get("/hello")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Gaston"));
        RestAssured.given()
                .get("/hello")
                .then()
                .statusCode(401);
    }

    @Test
    public void testUnhandledRuntimeException() {
        RestAssured.given()
                .auth().preemptive().basic("gaston", "gaston-password")
                .header("fail-unhandled", "true")
                .get("/hello")
                .then()
                .statusCode(500)
                .body(Matchers.containsString(UnhandledRuntimeException.class.getName()))
                .body(Matchers.containsString("Expected unhandled failure"));
    }

    @Test
    public void testCustomExceptionInIdentityProvider() {
        RestAssured.given()
                .auth().preemptive().basic("gaston", "gaston-password")
                .header("fail-authentication", "true")
                .get("/hello")
                .then()
                .statusCode(500)
                .body(Matchers.is("Expected authentication failure"));
    }

    @Test
    public void testCustomExceptionInIdentityAugmentor() {
        RestAssured.given()
                .auth().preemptive().basic("gaston", "gaston-password")
                .header("fail-augmentation", "true")
                .get("/hello")
                .then()
                .statusCode(500)
                .body(Matchers.is("Expected identity augmentation failure"));
    }

    @Path("/hello")
    public static class HelloResource {
        @GET
        public String hello(@Context SecurityContext context) {
            var principalName = context.getUserPrincipal() == null ? "" : " " + context.getUserPrincipal().getName();
            return "Hello" + principalName;
        }
    }

    public static class Mappers {
        @ServerExceptionMapper(CustomRuntimeException.class)
        public Response toResponse(CustomRuntimeException exception) {
            return Response.serverError().entity(exception.getMessage()).build();
        }
    }

    @ApplicationScoped
    public static class CustomIdentityAugmentor implements SecurityIdentityAugmentor {
        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
                AuthenticationRequestContext authenticationRequestContext) {
            return augment(securityIdentity, authenticationRequestContext, Map.of());
        }

        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context,
                Map<String, Object> attributes) {
            final RoutingContext routingContext = HttpSecurityUtils.getRoutingContextAttribute(attributes);
            if (routingContext.request().headers().contains("fail-augmentation")) {
                return Uni.createFrom().failure(new CustomRuntimeException("Expected identity augmentation failure"));
            }
            return Uni.createFrom().item(identity);
        }
    }

    public static class CustomRuntimeException extends RuntimeException {
        public CustomRuntimeException(String message) {
            super(message);
        }
    }

    public static class UnhandledRuntimeException extends RuntimeException {
        public UnhandledRuntimeException(String message) {
            super(message);
        }
    }

    @ApplicationScoped
    public static class BasicIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

        @Override
        public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
            return UsernamePasswordAuthenticationRequest.class;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest authRequest,
                AuthenticationRequestContext authRequestCtx) {
            if (!"gaston".equals(authRequest.getUsername())) {
                return Uni.createFrom().nullItem();
            }

            final RoutingContext routingContext = HttpSecurityUtils.getRoutingContextAttribute(authRequest);
            if (routingContext.request().headers().contains("fail-authentication")) {
                return Uni.createFrom().failure(new CustomRuntimeException("Expected authentication failure"));
            }
            if (routingContext.request().headers().contains("fail-unhandled")) {
                return Uni.createFrom().failure(new UnhandledRuntimeException("Expected unhandled failure"));
            }
            return Uni.createFrom()
                    .item(QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal("Gaston")).build());
        }
    }
}
