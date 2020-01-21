package io.quarkus.it.vertx.tcp;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.net.NetClient;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetClientOptions;

@Path("/tcp")
@Produces(MediaType.TEXT_PLAIN)
public class VertxTcpEndpoint {

    @Inject
    Vertx vertx;

    private NetClient client;
    private NetClient ssl;

    @PostConstruct
    public void init() {
        client = vertx.createNetClient();
        NetClientOptions options = new NetClientOptions()
                .setSsl(true)
                .setTrustStoreOptions(new JksOptions()
                        .setPath("src/main/resources/client-truststore.jks")
                        .setPassword("wibble"))
                .setKeyCertOptions(new JksOptions()
                        .setPath("src/main/resources/client-keystore.jks")
                        .setPassword("wibble"));
        ssl = vertx.createNetClient(options);
    }

    @POST
    public CompletionStage<String> ping(String body) {

        return client.connect(4321, "localhost")
                .thenCompose(socket -> {
                    CompletableFuture<String> response = new CompletableFuture<>();
                    socket.handler(buffer -> {
                        response.complete(buffer.toString());
                        socket.close();
                    }).write(body);
                    return response;
                });
    }

    @POST
    @Path("/ssl")
    public CompletionStage<String> ssl(String body) {
        return ssl.connect(4322, "localhost")
                .thenCompose(socket -> {
                    CompletableFuture<String> response = new CompletableFuture<>();
                    socket.handler(buffer -> {
                        response.complete(buffer.toString());
                        socket.close();
                    }).write(body);
                    return response;
                });
    }
}
