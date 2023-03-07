package org.jboss.resteasy.reactive.server.vertx.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Base64;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.smallrye.mutiny.Uni;

public class BodyPayloadBlockingAllowedTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestResource.class));

    @Test
    void testSmallRequestForNonBlocking() {
        doTest(5_000, "non-blocking", false);
    }

    @Test
    void testLargeRequestForNonBlocking() {
        doTest(5_000_000, "non-blocking", false);
    }

    @Test
    void testSmallRequestForBlocking() {
        doTest(5_000, "blocking", true);
    }

    @Test
    void testLargeRequestForBlocking() {
        doTest(5_000_000, "blocking", true);
    }

    private static void doTest(int size, String path, boolean blockingAllowed) {
        given() //
                .body(String.format("{\"data\":\"%s\"}", getBase64String(size)))
                .header("Content-Type", MediaType.TEXT_PLAIN)
                .when().post("/test/" + path)
                .then()
                .statusCode(200)
                .body(equalTo("" + blockingAllowed));
    }

    private static String getBase64String(int size) {
        return Base64.getEncoder().encodeToString(new byte[size]);
    }

    @Path("test")
    public static class TestResource {

        @Path("non-blocking")
        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public Uni<Boolean> nonBlocking(String request) {
            return Uni.createFrom().item(BlockingOperationSupport::isBlockingAllowed);
        }

        @Path("blocking")
        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public Boolean blocking(String request) {
            return BlockingOperationSupport.isBlockingAllowed();
        }

    }
}
