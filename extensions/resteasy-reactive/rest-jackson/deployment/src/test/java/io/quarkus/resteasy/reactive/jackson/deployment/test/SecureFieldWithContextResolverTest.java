package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.with;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.http.ContentType;
import tools.jackson.databind.ObjectMapper;

public class SecureFieldWithContextResolverTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Alpha.class, Beta.class, PlainBeta.class, AlphaResource.class, BetaResource.class,
                            PlainBetaResource.class, PerTypeMapperResolver.class))
            // build time generated serializers ignore mix-ins added to the ObjectMapper, so we disable them
            // in order to have the ContextResolver mix-ins take effect
            .overrideConfigKey("quarkus.rest.jackson.optimization.enable-reflection-free-serializers", "false");

    @Test
    public void test() {
        // the order matters: the first request to a @SecureField endpoint used to pin the ObjectWriter for all
        // subsequent @SecureField responses, regardless of the ObjectMapper returned by the ContextResolver
        with().accept(ContentType.JSON)
                .get("alpha")
                .then()
                .statusCode(200)
                .body(not(containsString("admin")))
                .body(not(containsString("some_field")))
                .body(containsString("someField"));

        with().accept(ContentType.JSON)
                .get("beta")
                .then()
                .statusCode(200)
                .body(not(containsString("admin")))
                .body(containsString("some_field"))
                .body(not(containsString("someField")));

        // control: same mapper as beta but no @SecureField
        with().accept(ContentType.JSON)
                .get("plain-beta")
                .then()
                .statusCode(200)
                .body(not(containsString("admin")))
                .body(containsString("some_field"))
                .body(not(containsString("someField")));
    }

    public static class Alpha {
        public String name = "alpha";
        public String someField = "ALPHA";
        @SecureField(rolesAllowed = "admin")
        public String adminOnly = "alpha-admin-only";
    }

    public static class Beta {
        public String name = "beta";
        public String someField = "BETA";
        @SecureField(rolesAllowed = "admin")
        public String adminOnly = "beta-admin-only";
    }

    public static class PlainBeta {
        public String name = "plain-beta";
        public String someField = "PLAIN-BETA";
    }

    public abstract static class SomeMixin {
        @JsonProperty("some_field")
        public String someField;
    }

    @Path("/alpha")
    public static class AlphaResource {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Alpha get() {
            return new Alpha();
        }
    }

    @Path("/beta")
    public static class BetaResource {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Beta get() {
            return new Beta();
        }
    }

    @Path("/plain-beta")
    public static class PlainBetaResource {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public PlainBeta get() {
            return new PlainBeta();
        }
    }

    @Provider
    public static class PerTypeMapperResolver implements ContextResolver<ObjectMapper> {

        private final ObjectMapper withoutMixin;
        private final ObjectMapper withMixin;

        public PerTypeMapperResolver(ObjectMapper base) {
            this.withoutMixin = base.rebuild().build();
            this.withMixin = base.rebuild()
                    .addMixIn(Beta.class, SomeMixin.class)
                    .addMixIn(PlainBeta.class, SomeMixin.class)
                    .build();
        }

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return (Beta.class.equals(type) || PlainBeta.class.equals(type)) ? withMixin : withoutMixin;
        }
    }
}
