package io.quarkus.resteasy.reactive.links.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.test.QuarkusUnitTest;

public class RestLinksCollectionTypeHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestRecord.class, AbstractEntity.class, AbstractId.class, Resource.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Path("/records")
    public static class Resource {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @RestLink(rel = "links")
        @InjectRestLinks
        public List<TestRecord> getAll() {
            return List.of(new TestRecord(1, "one", "v1"), new TestRecord(2, "two", "v2"));
        }
    }

    @Test
    public void test() {
        given()
                .when().get("/records")
                .then()
                .statusCode(200)
                .header("Link", containsString("rel=\"links\""));
    }
}
