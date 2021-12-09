package io.quarkus.test.integration.mtls;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.Startup;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;

@Singleton
@Startup
public class ExternalServer {

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
