package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import static io.restassured.RestAssured.when;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class StreamingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(StreamingResource.class));

    @Test
    public void testSseMultiJsonString() {
        when().get("/test/multi")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("[\"Hello\",\"Hola\"]"));
    }

    @Path("/test")
    public static class StreamingResource {

        @GET
        @Path("multi")
        @Produces(MediaType.APPLICATION_JSON)
        public Multi<String> multi() {
            return Multi.createFrom().items("Hello", "Hola");
        }
    }

}
