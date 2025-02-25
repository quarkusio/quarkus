package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.NonBlocking;

// Ensures uncommon field names like "set", "get", and "is" are generated correctly.
class FieldNameSetGetPrefixResourceTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Resource.class, Resource.UncommonBody.class).addAsResource(
                            new StringAsset(
                                    "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                            "application.properties"));

    @Test
    void testFieldNameSetGetIsPrefix() {
        RestAssured.get("/field-name-prefixes")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("id", Matchers.equalTo("id"))
                .body("set", Matchers.is(true))
                .body("get", Matchers.is(true))
                .body("is", Matchers.is(false))
                .body("setText", Matchers.equalTo("setText"));
    }

    @NonBlocking
    @Path("/field-name-prefixes")
    private static class Resource {
        @GET
        public UncommonBody get() {
            return new UncommonBody("id", true, true, false, "setText");
        }

        private record UncommonBody(String id, boolean set, boolean get, boolean is, String setText) {
        }
    }

}
