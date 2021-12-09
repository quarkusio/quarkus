package io.quarkus.test.integration.mtls;

import static io.restassured.RestAssured.when;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.Startup;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;

@QuarkusTest
public class MutualTLSTest {

    @Test
    public void testViaExternalServer() {
        when()
                .get("http://localhost:8080")
                .then()
                .statusCode(200)
                .log().ifValidationFails();
    }

    @Singleton
    @Startup
    public static class ExternalServer {

        HttpServer httpServer;

        @Inject
        public ExternalServer(Vertx vertx, @RestClient TestAPI testAPI, ObjectMapper objectMapper, Logger logger) {
            httpServer = vertx.createHttpServer()
                    .requestHandler(request -> {
                        logger.info("Handling External Request");
                        testAPI.getNames()
                                .flatMap(names -> {
                                    var response = request.response();
                                    try {
                                        String responseJson = objectMapper.writeValueAsString(names);
                                        response.setStatusCode(200);
                                        response.putHeader("Content-Type", "application/json");
                                        return response.end(responseJson);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        response.setStatusCode(500);
                                        response.putHeader("Content-Type", "text/plain");
                                        var out = new StringWriter();
                                        e.printStackTrace(new PrintWriter(out));
                                        return response.end(out.toString());
                                    }
                                })
                                .onFailure().recoverWithUni(e -> {
                                    e.printStackTrace();
                                    var response = request.response();
                                    response.setStatusCode(500);
                                    response.putHeader("Content-Type", "text/plain");
                                    var out = new StringWriter();
                                    e.printStackTrace(new PrintWriter(out));
                                    return response.end(out.toString());
                                })
                                .subscribe().asCompletionStage();
                    })
                    .listen(8080)
                    .onItem().invoke(() -> logger.info("External Server Listening"))
                    .await().indefinitely();
        }

    }

    @RegisterRestClient(configKey = "test-api")
    @Path("test")
    @Produces(APPLICATION_JSON)
    public static interface TestAPI {

        @GET
        @Path("names")
        Uni<List<String>> getNames();

    }

    @Path("test")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public static class TestResource {

        @GET
        @Path("names")
        public Uni<List<String>> getNames() {
            return Uni.createFrom().item(List.of("a", "b", "c", "d"));
        }

    }
}
