package io.quarkus.resteasy.reactive.server.test.duplicate;

import static io.restassured.RestAssured.when;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.Mock;
import io.quarkus.test.QuarkusUnitTest;

public class DuplicateResourceAndClientTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Client.class, Resource.class));

    @Test
    public void dummy() {
        when()
                .get("/hello")
                .then()
                .statusCode(200);
    }

    @Path("/hello")
    public interface Client {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        String hello();
    }

    @Mock
    public static class ClientMock implements Client {

        @Override
        public String hello() {
            return "";
        }
    }

    @Path("/hello")
    public static class Resource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "hello";
        }
    }
}
