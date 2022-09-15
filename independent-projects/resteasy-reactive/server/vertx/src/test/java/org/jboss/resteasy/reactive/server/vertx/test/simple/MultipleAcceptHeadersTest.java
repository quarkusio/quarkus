package org.jboss.resteasy.reactive.server.vertx.test.simple;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MultipleAcceptHeadersTest {

    private static final String BODY = "{\"message\": \"hello world\"}";

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloResource.class));

    @Test
    public void matchingHeaderIsFirst() throws Exception {
        // the WebClient is used because RestAssured can't seem to send the 'Accept' header multiple times...
        WebClient client = WebClient.create(Vertx.vertx());

        var response = client.get(ResteasyReactiveUnitTest.SERVER_PORT, "localhost", "/hello")
                .putHeader("Accept", List.of("application/xml", "application/json")).send().toCompletionStage()
                .toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.bodyAsString()).isEqualTo(BODY);
    }

    @Test
    public void matchingHeaderIsLast() throws Exception {
        WebClient client = WebClient.create(Vertx.vertx());

        var response = client.get(ResteasyReactiveUnitTest.SERVER_PORT, "localhost", "/hello")
                .putHeader("Accept", List.of("application/json", "application/xml")).send().toCompletionStage()
                .toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.bodyAsString()).isEqualTo(BODY);
    }

    @Path("/hello")
    public static class HelloResource {

        @GET
        @Produces("application/json")
        public String hello() {
            return BODY;
        }
    }
}
