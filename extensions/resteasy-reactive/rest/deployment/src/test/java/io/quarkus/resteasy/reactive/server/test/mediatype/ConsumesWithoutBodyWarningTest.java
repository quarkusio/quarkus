package io.quarkus.resteasy.reactive.server.test.mediatype;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * {@code @Consumes} declared on a method whose HTTP method carries no request body has no effect on the body handling,
 * so the build logs a warning for it.
 */
public class ConsumesWithoutBodyWarningTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Resource.class))
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.WARNING)
                    && record.getMessage().contains("@Consumes"))
            .assertLogRecords(records -> {
                assertThat(records).map(WarningMessages::format).hasSize(3);
                assertThat(records).map(WarningMessages::format)
                        .anyMatch(m -> m.contains("Method 'get'") && m.contains("GET"))
                        .anyMatch(m -> m.contains("Method 'head'") && m.contains("HEAD"))
                        .anyMatch(m -> m.contains("Method 'options'") && m.contains("OPTIONS"))
                        .noneMatch(m -> m.contains("Method 'post'"))
                        .noneMatch(m -> m.contains("Method 'delete'"));
            });

    @Test
    public void getWithoutContentTypeIsMatched() {
        get("/test").then().statusCode(200);
    }

    @Test
    public void getWithOtherContentTypeIsRejected() {
        given().contentType(MediaType.TEXT_PLAIN).get("/test").then().statusCode(415);
    }

    @Path("test")
    public static class Resource {

        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        public String get() {
            return "get";
        }

        @HEAD
        @Consumes(MediaType.APPLICATION_JSON)
        public void head() {
        }

        @OPTIONS
        @Consumes(MediaType.APPLICATION_JSON)
        public String options() {
            return "options";
        }

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        public String post(String body) {
            return body;
        }

        @DELETE
        @Consumes(MediaType.APPLICATION_JSON)
        public String delete(String body) {
            return body;
        }
    }
}
