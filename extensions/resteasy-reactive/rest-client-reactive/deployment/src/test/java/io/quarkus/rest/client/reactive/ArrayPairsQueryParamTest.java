package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

public class ArrayPairsQueryParamTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Inject
    Vertx vertx;

    Client client;
    HttpServer server;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        setupServer();

        client = createClient();
    }

    @AfterEach
    void shutDown() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        server.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

        ((Closeable) client).close();
    }

    @Test
    void shouldPassMultiParamWithBrackets() {
        String response = client.getWithParams(List.of("one", "two"));

        assertThat(response).contains("my-param[]=one").contains("my-param[]=two");
    }

    @Test
    void shouldPassSingleParamWithoutBrackets() {
        String response = client.getWithParams(List.of("one"));

        assertThat(response).isEqualTo("my-param=one");
    }

    @Path("/")
    public interface Client {

        @GET
        String getWithParams(@QueryParam("my-param") List<String> paramValues);
    }

    private void setupServer() throws InterruptedException, ExecutionException {
        CompletableFuture<HttpServer> startResult = new CompletableFuture<>();
        vertx.createHttpServer().requestHandler(request -> request.response().setStatusCode(200).end(request.query()))
                .listen(8082)
                .onComplete(server -> {
                    if (server.failed()) {
                        startResult.completeExceptionally(server.cause());
                    } else {
                        startResult.complete(server.result());
                    }
                });
        server = startResult.get();
    }

    private Client createClient() {
        return RestClientBuilder.newBuilder()
                .queryParamStyle(QueryParamStyle.ARRAY_PAIRS)
                .baseUri(URI.create("http://localhost:8082"))
                .build(Client.class);
    }
}
