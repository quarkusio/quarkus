package org.jboss.shamrock.vertx.tests;


import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;

@Path("/")
public class VertxProducerResource {

    @Inject
    private Vertx vertx;

    @Inject
    private EventBus eventBus;

    @Path("https-server")
    @GET
    public CompletionStage<String> httpsServer() throws CertificateException{
        SelfSignedCertificate cert = SelfSignedCertificate.create("localhost");
        HttpServer server = vertx.createHttpServer(new HttpServerOptions()
                                                   .setSsl(true)
                                                   .setKeyCertOptions(cert.keyCertOptions())
                                                   .setTrustOptions(cert.trustOptions())
                                                   );
        server.requestHandler(req -> {
            req.response().end("OK");
        });

        CompletableFuture<String> cf = new CompletableFuture<>();
        server.listen(0, "localhost", res -> {
            if(res.failed()) {
                server.close();
                cf.completeExceptionally(res.cause());
            } else {
                WebClient client = WebClient.create(vertx, new WebClientOptions()
                                                    .setSsl(true)
                                                    .setKeyCertOptions(cert.keyCertOptions())
                                                    .setTrustOptions(cert.trustOptions()));
                client.get(server.actualPort(), "localhost", "/")
                .as(BodyCodec.string())
                .send(resp -> {
                    server.close();
                    if(resp.failed())
                        cf.completeExceptionally(resp.cause());
                    else
                        cf.complete(resp.result().body());
                });
            }
        });
        
        return cf;
    }
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Map<String, Boolean> test() {
        Map<String, Boolean> map = new HashMap<>();
        map.put("vertx", vertx != null);
        map.put("eventbus", eventBus != null);
        return map;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/eventBus")
    public CompletionStage<String> eb() {
        String address = UUID.randomUUID().toString();

        // Use the event bus bean.
        MessageConsumer<String> consumer = eventBus.consumer(address);
        consumer.handler(m -> {
            m.reply("hello " + m.body());
            consumer.unregister();
        });

        CompletableFuture<String> future = new CompletableFuture<>();
        // Use the Vert.x bean.
        vertx.eventBus().<String>send(address, "shamrock", ar -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
            } else {
                future.complete(ar.result().body());
            }
        });

        return future;
    }


}
